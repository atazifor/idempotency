package com.example.idempotency.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("orders")
public record Order(
        @Id String id,
        String userId,
        String product,
        int quantity,
        double price,
        OrderStatus status,
        Instant createdAt
) {
    public Order withStatus(OrderStatus status) {
        return new Order(id, userId, product, quantity, price, status, createdAt);
    }
}
