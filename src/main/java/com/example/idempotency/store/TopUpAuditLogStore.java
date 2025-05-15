package com.example.idempotency.store;

import com.example.idempotency.model.TopUpAuditEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TopUpAuditLogStore {
    private final Map<String, List<TopUpAuditEntry>> userLogs = new ConcurrentHashMap<>();

    public void save(TopUpAuditEntry entry) {
        userLogs.computeIfAbsent(entry.getUserId(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(entry);
    }

    public List<TopUpAuditEntry> getLogForUser(String userId) {
        return userLogs.getOrDefault(userId, Collections.emptyList());
    }
}
