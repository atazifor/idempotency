package com.example.idempotency.service;

import com.example.idempotency.model.Order;
import com.example.idempotency.model.OrderStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OrderStateService {
    public Mono<Order> pay(Order order) {
        if(order.status() != OrderStatus.PENDING) {
            return Mono.error(new IllegalStateException("Only PENDING orders can be paid."));
        }
        return Mono.just(order.withStatus(OrderStatus.PAID));
    }

    public Mono<Order> cancel(Order order) {
        if (order.status() != OrderStatus.PENDING) {
            return Mono.error(new IllegalStateException("Only PENDING orders can be cancelled."));
        }
        return Mono.just(order.withStatus(OrderStatus.CANCELLED));
    }
}
