package com.careeros.resumetailor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        boolean apiKeyEnabled,
        String apiKey,
        int rateLimitPerMinute,
        int tailorRateLimitPerHour
) {
    public boolean isApiKeyRequired() {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }
        return apiKeyEnabled;
    }
}
