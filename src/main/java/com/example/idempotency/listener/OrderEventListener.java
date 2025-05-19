package com.example.idempotency.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventListener {
    private final static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OrderEventListener.class);
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-events", groupId = "order-event-log")
    public void listen(String message) {
        logger.info("RAW MESSAGE RECEIVED: {}", message);
        JsonNode event = null;
        try {
            event = objectMapper.readTree(message);
            // If outer is a quoted string: "{\"eventType\":\"order-created\"...}"
            if (event.isTextual()) {
                logger.info("Detected double-encoded JSON, unwrapping...");
                event = objectMapper.readTree(event.asText());  // unwrap it
            }
            logger.info("Order event received: {}", event);
            String eventType = event.path("eventType").asText(null);
            logger.info("Parsed eventType = {}", eventType);
            String orderId = event.get("orderId").asText();
            String status = event.get("status").asText();
            logger.info("Order event received: eventType={}, orderId={}, status={}", eventType, orderId, status);
        } catch (JsonProcessingException e) {
            logger.error("Failed to process order event: {}", e.getMessage());
        }
    }
}
