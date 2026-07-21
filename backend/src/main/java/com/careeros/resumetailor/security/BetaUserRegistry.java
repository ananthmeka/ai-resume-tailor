package com.careeros.resumetailor.security;

import com.careeros.resumetailor.config.SecurityProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class BetaUserRegistry {

    private final Map<String, String> tokenHashToUser;

    public BetaUserRegistry(SecurityProperties security) {
        Map<String, String> users = new LinkedHashMap<>();
        String configured = security.betaUsers();
        if (configured != null && !configured.isBlank()) {
            for (String entry : configured.split(",")) {
                String[] parts = entry.trim().split(":", 2);
                if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                    users.put(hash(parts[1].trim()), parts[0].trim());
                }
            }
        }
        this.tokenHashToUser = Map.copyOf(users);
    }

    public Optional<String> authenticate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        byte[] candidate = hash(token.trim()).getBytes(StandardCharsets.UTF_8);
        return tokenHashToUser.entrySet().stream()
                .filter(entry -> MessageDigest.isEqual(
                        candidate, entry.getKey().getBytes(StandardCharsets.UTF_8)))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public int configuredUsers() {
        return tokenHashToUser.size();
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
