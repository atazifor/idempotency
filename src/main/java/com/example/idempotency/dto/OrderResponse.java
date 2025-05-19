package com.example.idempotency.dto;

import com.example.idempotency.model.Order;
import com.example.idempotency.model.OrderStatus;

import java.time.Instant;

public record OrderResponse(
        String id,
        String userId,
        String product,
        int quantity,
        double price,
        OrderStatus status,
        Instant createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.id(),
                order.userId(),
                order.product(),
                order.quantity(),
                order.price(),
                order.status(),
                order.createdAt()
        );
    }
}
