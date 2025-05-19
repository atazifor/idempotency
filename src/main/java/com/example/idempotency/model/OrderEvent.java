package com.example.idempotency.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("order_events")
public record OrderEvent(
        @Id String id,
        String eventType,
        String orderId,
        String userId,
        String status,
        Instant timestamp
) {}