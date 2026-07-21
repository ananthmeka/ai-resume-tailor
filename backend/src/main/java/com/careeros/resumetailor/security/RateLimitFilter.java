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
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    private final SecurityProperties security;
    private final InMemoryRateLimiter limiter;
    private final PersistentQuotaStore quotaStore;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(SecurityProperties security, InMemoryRateLimiter limiter,
                           PersistentQuotaStore quotaStore, ObjectMapper objectMapper) {
        this.security = security;
        this.limiter = limiter;
        this.quotaStore = quotaStore;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
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
        String ip = ClientIpResolver.resolve(request);
        String path = request.getRequestURI();

        boolean tailor = path != null && (path.startsWith("/api/tailor"));
        if (tailor) {
            Object authenticated = request.getAttribute("authenticatedUser");
            if (authenticated != null) {
                var result = quotaStore.consumeTailor(authenticated.toString(),
                        security.tailorRateLimitPerHour(), security.tailorRateLimitPerMonth());
                response.setHeader("X-Quota-Hour-Remaining", Integer.toString(result.hourlyRemaining()));
                response.setHeader("X-Quota-Month-Remaining", Integer.toString(result.monthlyRemaining()));
                if (!result.allowed()) {
                    writeError(response, 429, "Your beta resume-generation quota has been reached.");
                    return;
                }
            } else {
                String key = "tailor:" + ip;
                if (!limiter.tryConsume(key, security.tailorRateLimitPerHour(), 3_600_000L)) {
                    writeError(response, 429, "Tailor limit reached. Try again later.");
                    return;
                }
            }
        }

        String globalKey = "global:" + ip;
        if (!limiter.tryConsume(globalKey, security.rateLimitPerMinute(), 60_000L)) {
            writeError(response, 429, "Too many requests. Slow down.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of("error", message));
    }
}
