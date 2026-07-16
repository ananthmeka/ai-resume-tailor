package com.careeros.resumetailor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.llm-limits")
public record LlmLimits(
        int maxResumeChars,
        int maxJdChars,
        int maxStructuredJsonChars,
        int maxResumeCharsFallback,
        int preferFallbackOverChars,
        int pauseMsBetweenLlmCalls
) {
    public LlmLimits {
        if (maxResumeCharsFallback <= 0) {
            maxResumeCharsFallback = Math.max(6000, maxResumeChars / 2);
        }
    }
}
