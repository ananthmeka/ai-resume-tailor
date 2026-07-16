package com.careeros.resumetailor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiChatService {

    private final RestClient primaryClient;
    private final ObjectMapper objectMapper;
    private final String primaryApiKey;
    private final String primaryModel;
    private final boolean requireApiKey;
    private final boolean jsonResponseFormat;
    private final int maxCompletionTokens;

    private final boolean fallbackEnabled;
    private final RestClient fallbackClient;
    private final String fallbackApiKey;
    private final String fallbackModel;
    private final int fallbackMaxCompletionTokens;
    private final boolean fallbackJsonFormat;
    private final int preferFallbackOverChars;

    public OpenAiChatService(
            RestClient openAiRestClient,
            ObjectMapper objectMapper,
            @Value("${app.openai.api-key}") String apiKey,
            @Value("${app.openai.model}") String model,
            @Value("${app.openai.require-api-key:true}") boolean requireApiKey,
            @Value("${app.openai.json-response-format:true}") boolean jsonResponseFormat,
            @Value("${app.openai.max-completion-tokens:8192}") int maxCompletionTokens,
            @Value("${app.openai.fallback.enabled:false}") boolean fallbackEnabled,
            @Value("${app.openai.fallback.base-url:}") String fallbackBaseUrl,
            @Value("${app.openai.fallback.api-key:}") String fallbackApiKey,
            @Value("${app.openai.fallback.model:}") String fallbackModel,
            @Value("${app.openai.fallback.max-completion-tokens:8192}") int fallbackMaxCompletionTokens,
            @Value("${app.openai.fallback.json-response-format:true}") boolean fallbackJsonFormat,
            @Value("${app.openai.fallback.timeout-seconds:180}") int fallbackTimeoutSeconds,
            @Value("${app.llm-limits.prefer-fallback-over-chars:9000}") int preferFallbackOverChars) {
        this.primaryClient = openAiRestClient;
        this.objectMapper = objectMapper;
        this.primaryApiKey = apiKey;
        this.primaryModel = model;
        this.requireApiKey = requireApiKey;
        this.jsonResponseFormat = jsonResponseFormat;
        this.maxCompletionTokens = maxCompletionTokens;
        this.fallbackEnabled = fallbackEnabled && fallbackBaseUrl != null && !fallbackBaseUrl.isBlank();
        this.fallbackApiKey = fallbackApiKey;
        this.fallbackModel = fallbackModel;
        this.fallbackMaxCompletionTokens = fallbackMaxCompletionTokens;
        this.fallbackJsonFormat = fallbackJsonFormat;
        this.preferFallbackOverChars = preferFallbackOverChars;
        if (this.fallbackEnabled) {
            this.fallbackClient = RestClient.builder()
                    .baseUrl(fallbackBaseUrl)
                    .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                        setConnectTimeout(Duration.ofSeconds(fallbackTimeoutSeconds));
                        setReadTimeout(Duration.ofSeconds(fallbackTimeoutSeconds));
                    }})
                    .build();
        } else {
            this.fallbackClient = null;
        }
    }

    public boolean isFallbackConfigured() {
        return fallbackEnabled;
    }

    public String chatJson(String systemPrompt, String userPrompt) {
        return chatJson(systemPrompt, userPrompt, userPrompt != null ? userPrompt.length() : 0);
    }

    /** Large inputs (e.g. two-page resumes) can use fallback provider first when configured. */
    public String chatJson(String systemPrompt, String userPrompt, int estimatedInputChars) {
        if (requireApiKey && (primaryApiKey == null || primaryApiKey.isBlank())) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured");
        }
        boolean useFallbackFirst = fallbackEnabled && estimatedInputChars >= preferFallbackOverChars;
        if (useFallbackFirst) {
            try {
                return invoke(fallbackClient, fallbackApiKey, fallbackModel, fallbackMaxCompletionTokens, fallbackJsonFormat,
                        systemPrompt, userPrompt);
            } catch (IllegalStateException e) {
                if (!LlmInputTruncator.isTokenLimitError(e)) {
                    throw e;
                }
            }
        }
        try {
            return invokeWithRateLimitRetry(primaryClient, primaryApiKey, primaryModel, maxCompletionTokens, jsonResponseFormat,
                    systemPrompt, userPrompt);
        } catch (IllegalStateException e) {
            if (fallbackEnabled && LlmInputTruncator.isTokenLimitError(e)) {
                return invoke(fallbackClient, fallbackApiKey, fallbackModel, fallbackMaxCompletionTokens, fallbackJsonFormat,
                        systemPrompt, userPrompt);
            }
            throw e;
        }
    }

    private String invokeWithRateLimitRetry(
            RestClient client,
            String apiKey,
            String model,
            int maxTokens,
            boolean jsonFormat,
            String systemPrompt,
            String userPrompt) {
        IllegalStateException last = null;
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                return invoke(client, apiKey, model, maxTokens, jsonFormat, systemPrompt, userPrompt);
            } catch (IllegalStateException e) {
                last = e;
                if (!LlmInputTruncator.isTokenLimitError(e) || attempt >= 3) {
                    throw e;
                }
                long wait = LlmInputTruncator.retryDelayMillis(e.getMessage(), 12_000);
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        throw last != null ? last : new IllegalStateException("LLM request failed");
    }

    private String invoke(
            RestClient client,
            String apiKey,
            String model,
            int maxTokens,
            boolean jsonFormat,
            String systemPrompt,
            String userPrompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.2);
        if (jsonFormat) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        body.put("messages", messages);
        if (maxTokens > 0) {
            body.put("max_completion_tokens", maxTokens);
        }

        try {
            RestClient.RequestBodySpec spec = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isBlank()) {
                spec = spec.header("Authorization", "Bearer " + apiKey);
            }
            String response = spec.body(body).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new IllegalStateException("Empty response from AI provider");
            }
            return extractJsonObject(content.asText().trim());
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("AI provider error: " + e.getResponseBodyAsString(), e);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("AI request failed: " + e.getMessage(), e);
        }
    }

    private static String extractJsonObject(String text) {
        if (text.startsWith("```")) {
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return text.substring(start, end + 1);
            }
        }
        return text;
    }
}
