package com.careeros.resumetailor.security;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryRateLimiter {

    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    /**
     * @return true if allowed, false if limit exceeded
     */
    public synchronized boolean tryConsume(String key, int maxEvents, long windowMillis) {
        long now = System.currentTimeMillis();
        Deque<Long> deque = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        while (!deque.isEmpty() && now - deque.peekFirst() > windowMillis) {
            deque.pollFirst();
        }
        if (deque.size() >= maxEvents) {
            return false;
        }
        deque.addLast(now);
        return true;
    }
}
