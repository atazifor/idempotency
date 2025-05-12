package com.example.idempotency.store;

import com.example.idempotency.model.TopUpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IdempotencyStore {
    private static final Logger logger = LoggerFactory.getLogger(IdempotencyStore.class);
    public static class Entry {
        TopUpResponse response;
        Instant timestamp;

        public Entry(TopUpResponse response) {
            this.response = response;
            this.timestamp = Instant.now();
        }
    }

    private final Map<String, Entry> processedRequests = new ConcurrentHashMap<>();
    private final Duration ttl = Duration.ofSeconds(20);

    public TopUpResponse getTopUpResponse(String key) {
        removeIfExpired(key);
        return processedRequests.getOrDefault(key, new Entry(null)).response;
    }

    public boolean contains(String key) {
        removeIfExpired(key);
        return processedRequests.containsKey(key);
    }

    public void store(String key, TopUpResponse topUpResponse) {
        processedRequests.put(key, new Entry(topUpResponse));
    }

    public void removeIfExpired(String key) {
        Entry entry = processedRequests.get(key);
        if(entry != null && entry.timestamp.plus(ttl).isBefore(Instant.now())) {
            logger.info("[EXPIRED] key={}", key);
            processedRequests.remove(key);
        }
    }
}
