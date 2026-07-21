package com.careeros.resumetailor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        boolean apiKeyEnabled,
        String apiKey,
        boolean betaAccessEnabled,
        String betaUsers,
        int rateLimitPerMinute,
        int tailorRateLimitPerHour,
        int tailorRateLimitPerMonth
) {
    public boolean isApiKeyRequired() {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }
        return apiKeyEnabled;
    }

    public boolean isAuthenticationRequired() {
        return isApiKeyRequired() || betaAccessEnabled;
    }
}
