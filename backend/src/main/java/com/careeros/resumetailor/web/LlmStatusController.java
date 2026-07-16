package com.careeros.resumetailor.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class LlmStatusController {

    @GetMapping("/llm-status")
    public Map<String, Object> llmStatus(
            @Value("${app.openai.base-url}") String baseUrl,
            @Value("${app.openai.model}") String model,
            @Value("${app.openai.require-api-key:true}") boolean requireApiKey,
            @Value("${app.openai.fallback.enabled:false}") boolean fallbackEnabled) {
        return Map.of(
                "provider", providerLabel(baseUrl),
                "model", model,
                "baseUrlHost", hostOnly(baseUrl),
                "requireApiKey", requireApiKey,
                "activeProfiles", profiles,
                "fallbackConfigured", fallbackEnabled);
    }

    private static String providerLabel(String baseUrl) {
        if (baseUrl == null) {
            return "unknown";
        }
        if (baseUrl.contains("11434") || baseUrl.contains("ollama")) {
            return "ollama-compatible";
        }
        if (baseUrl.contains("openai.com")) {
            return "openai";
        }
        return "openai-compatible";
    }

    private static String hostOnly(String baseUrl) {
        try {
            return java.net.URI.create(baseUrl).getHost();
        } catch (Exception e) {
            return "configured";
        }
    }
}
