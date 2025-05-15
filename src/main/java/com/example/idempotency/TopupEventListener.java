package com.example.idempotency;

import com.example.idempotency.model.TopUpAuditEntry;
import com.example.idempotency.store.TopUpAuditLogStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TopupEventListener {
    private final static Logger logger = org.slf4j.LoggerFactory.getLogger(TopupEventListener.class);

    private final ObjectMapper objectMapper;
    private final TopUpAuditLogStore auditLogStore;

    @KafkaListener(topics = "topup-events", groupId = "topup-monitor")
    public void logTopUpEvent(String jsonMessage) throws JsonProcessingException {
        logger.info("[TOP-UP AUDIT] -> {}", jsonMessage);
        //store audit info
        TopUpAuditEntry entry = objectMapper.readValue(jsonMessage, TopUpAuditEntry.class);
        auditLogStore.save(entry);
    }

    @KafkaListener(topics = "topup-events", groupId = "rewards-monitor")
    public void assignPoints(String jsonMessage) throws JsonProcessingException {

        TopUpAuditEntry entry = objectMapper.readValue(jsonMessage, TopUpAuditEntry.class);
        if("NEW".equals(entry.getStatus()) && entry.getAmount() > 1000) {
            logger.info("[ASSIGN BONUS POINTS] userId={}, amount={}", entry.getUserId(), entry.getAmount());
        }
    }

    @KafkaListener(topics = "topup-events", groupId = "rate-limit-monitor")
    public void monitorRateLimit(String jsonMessage) throws JsonProcessingException {
        TopUpAuditEntry entry = objectMapper.readValue(jsonMessage, TopUpAuditEntry.class);
        if("RATE_LIMITED".equals(entry.getStatus())) {
            logger.info("⚠️ Warning: user {} is hitting the rate limit!", entry.getUserId());
        }
    }
}
