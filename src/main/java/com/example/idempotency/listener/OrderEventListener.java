package com.example.idempotency.listener;

import com.example.idempotency.model.OrderEvent;
import com.example.idempotency.repository.OrderEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class OrderEventListener {
    private final static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OrderEventListener.class);

    private final ObjectMapper objectMapper;
    private final OrderEventRepository repository;

    @KafkaListener(topics = "order-events", groupId = "order-event-log")
    public void listen(String message) {
        logger.info("RAW MESSAGE RECEIVED: {}", message);
        JsonNode event;
        try {
            event = objectMapper.readTree(message);
            // If outer is a quoted string: "{\"eventType\":\"order-created\"...}"
            if (event.isTextual()) {
                logger.info("Detected double-encoded JSON, unwrapping...");
                event = objectMapper.readTree(event.asText());  // unwrap it
            }
            OrderEvent orderEvent = new OrderEvent(
                    null,
                    event.path("eventType").asText(),
                    event.get("orderId").asText(),
                    event.get("userId").asText(),
                    event.get("status").asText(),
                    Instant.now()
            );
            repository.save(orderEvent)
                    .doOnSuccess(saved -> logger.info("Order event saved: {}", saved))
                    .doOnError(e -> logger.error("Failed to save order event: {}", e.getMessage()))
                    .subscribe();

        } catch (JsonProcessingException e) {
            logger.error("Failed to process order event: {}", e.getMessage());
        }
    }
}
