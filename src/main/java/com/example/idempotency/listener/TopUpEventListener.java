package com.example.idempotency.listener;

import com.example.idempotency.model.TopUpAuditEntry;
import com.example.idempotency.repository.TopUpAuditRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TopUpEventListener {
    private final static Logger logger = org.slf4j.LoggerFactory.getLogger(TopUpEventListener.class);

    private final ObjectMapper objectMapper;
    private final TopUpAuditRepository topUpAuditRepository;

    @KafkaListener(topics = "topup-events", groupId = "topup-monitor")
    public void logTopUpEvent(String jsonMessage) throws JsonProcessingException {
        logger.info("[TOP-UP AUDIT] -> {}", jsonMessage);
        //store audit info
        TopUpAuditEntry entry = objectMapper.readValue(jsonMessage, TopUpAuditEntry.class);
        topUpAuditRepository.save(entry)
                .doOnSuccess(saved -> logger.info("✅ Saved audit entry for userId={}", saved.userId()))
                .doOnError(e -> logger.error("❌ Failed to save audit entry: {}", e.getMessage()))
                .subscribe();
    }

    @KafkaListener(topics = "topup-events", groupId = "rewards-monitor")
    public void assignPoints(String jsonMessage) throws JsonProcessingException {

        TopUpAuditEntry entry = objectMapper.readValue(jsonMessage, TopUpAuditEntry.class);
        if("NEW".equals(entry.status()) && entry.amount() > 1000) {
            logger.info("[ASSIGN BONUS POINTS] userId={}, amount={}", entry.userId(), entry.amount());
        }
    }

    @KafkaListener(topics = "topup-events", groupId = "rate-limit-monitor")
    public void monitorRateLimit(String jsonMessage) throws JsonProcessingException {
        TopUpAuditEntry entry = objectMapper.readValue(jsonMessage, TopUpAuditEntry.class);
        if("RATE_LIMITED".equals(entry.status())) {
            logger.info("⚠️ Warning: user {} is hitting the rate limit!", entry.userId());
        }
    }
}
