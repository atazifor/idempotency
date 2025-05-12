package com.example.idempotency.store;

import com.example.idempotency.model.TopUpResponse;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IdempotencyStore {
    private final Map<String, TopUpResponse> processedRequests = new ConcurrentHashMap<>();

    public TopUpResponse getTopUpResponse(String key) {
        return processedRequests.get(key);
    }

    public boolean contains(String key) {
        return processedRequests.containsKey(key);
    }

    public void store(String key, TopUpResponse topUpResponse) {
        processedRequests.put(key, topUpResponse);
    }
}
