package com.careeros.resumetailor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiChatService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final boolean requireApiKey;
    private final boolean jsonResponseFormat;
    private final int maxCompletionTokens;

    public OpenAiChatService(
            RestClient openAiRestClient,
            ObjectMapper objectMapper,
            @Value("${app.openai.api-key}") String apiKey,
            @Value("${app.openai.model}") String model,
            @Value("${app.openai.require-api-key:true}") boolean requireApiKey,
            @Value("${app.openai.json-response-format:true}") boolean jsonResponseFormat,
            @Value("${app.openai.max-completion-tokens:16384}") int maxCompletionTokens) {
        this.restClient = openAiRestClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.requireApiKey = requireApiKey;
        this.jsonResponseFormat = jsonResponseFormat;
        this.maxCompletionTokens = maxCompletionTokens;
    }

    public String chatJson(String systemPrompt, String userPrompt) {
        if (requireApiKey && (apiKey == null || apiKey.isBlank())) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.2);
        if (jsonResponseFormat) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        body.put("messages", messages);
        if (maxCompletionTokens > 0) {
            body.put("max_completion_tokens", maxCompletionTokens);
        }

        try {
            RestClient.RequestBodySpec spec = restClient.post()
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
            String text = content.asText().trim();
            return extractJsonObject(text);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("AI provider error: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new IllegalStateException("AI request failed: " + e.getMessage(), e);
        }
    }

    /** Tolerate markdown fences from local models. */
    private String extractJsonObject(String text) {
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
