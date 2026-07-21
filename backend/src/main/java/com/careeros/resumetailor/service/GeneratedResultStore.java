package com.careeros.resumetailor.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeneratedResultStore {

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final int MAX_RESULTS = 50;
    private final Map<String, StoredResult> results = new ConcurrentHashMap<>();

    public String put(String owner, String html) {
        cleanup();
        if (results.size() >= MAX_RESULTS) {
            results.entrySet().stream()
                    .min(Comparator.comparing(entry -> entry.getValue().createdAt()))
                    .ifPresent(entry -> results.remove(entry.getKey()));
        }
        String id = UUID.randomUUID().toString();
        results.put(id, new StoredResult(owner, html, Instant.now()));
        return id;
    }

    public Optional<String> getHtml(String id, String owner) {
        cleanup();
        StoredResult result = results.get(id);
        if (result == null || !result.owner().equals(owner)) {
            return Optional.empty();
        }
        return Optional.of(result.html());
    }

    private void cleanup() {
        Instant cutoff = Instant.now().minus(TTL);
        results.entrySet().removeIf(entry -> entry.getValue().createdAt().isBefore(cutoff));
    }

    private record StoredResult(String owner, String html, Instant createdAt) {}
}
