package com.careeros.resumetailor.security;

import com.careeros.resumetailor.config.SecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@Order(1)
public class ApiKeyFilter extends OncePerRequestFilter {

    private final SecurityProperties security;
    private final BetaUserRegistry betaUsers;
    private final ObjectMapper objectMapper;

    public ApiKeyFilter(SecurityProperties security, BetaUserRegistry betaUsers, ObjectMapper objectMapper) {
        this.security = security;
        this.betaUsers = betaUsers;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!security.isAuthenticationRequired()) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return path != null && path.startsWith("/api/health");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String betaUser = authenticateBetaUser(request);
        if (betaUser != null) {
            request.setAttribute("authenticatedUser", betaUser);
            filterChain.doFilter(request, response);
            return;
        }
        if (matchesApiKey(request)) {
            request.setAttribute("authenticatedUser", "legacy-api-key");
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of("error", "Invalid or missing beta access code"));
    }

    private String authenticateBetaUser(HttpServletRequest request) {
        String token = request.getHeader("X-Beta-Token");
        if (token == null || token.isBlank()) {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                token = auth.substring(7).trim();
            }
        }
        return betaUsers.authenticate(token).orElse(null);
    }

    private boolean matchesApiKey(HttpServletRequest request) {
        String expected = security.apiKey();
        String header = request.getHeader("X-API-Key");
        if (header != null && constantTimeEquals(header.trim(), expected)) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        return false;
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
