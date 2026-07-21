package com.careeros.resumetailor.web;

import com.careeros.resumetailor.config.SecurityProperties;
import com.careeros.resumetailor.security.BetaUserRegistry;
import com.careeros.resumetailor.security.PersistentQuotaStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class BetaAccountController {

    private final SecurityProperties security;
    private final BetaUserRegistry betaUsers;
    private final PersistentQuotaStore quotas;

    public BetaAccountController(SecurityProperties security, BetaUserRegistry betaUsers, PersistentQuotaStore quotas) {
        this.security = security;
        this.betaUsers = betaUsers;
        this.quotas = quotas;
    }

    @GetMapping
    public Map<String, Object> account(HttpServletRequest request) {
        Object authenticated = request.getAttribute("authenticatedUser");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("authenticationRequired", security.isAuthenticationRequired());
        result.put("betaUserCount", betaUsers.configuredUsers());
        if (authenticated == null) {
            result.put("user", "local-user");
            result.put("hourlyRemaining", security.tailorRateLimitPerHour());
            result.put("monthlyRemaining", security.tailorRateLimitPerMonth());
            return result;
        }
        String user = authenticated.toString();
        var status = quotas.status(user, security.tailorRateLimitPerHour(), security.tailorRateLimitPerMonth());
        result.put("user", user);
        result.put("hourlyRemaining", status.hourlyRemaining());
        result.put("monthlyRemaining", status.monthlyRemaining());
        return result;
    }
}
