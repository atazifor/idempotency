package com.example.idempotency.repository;

import com.example.idempotency.model.OrderEvent;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.time.Instant;

public interface OrderEventRepository extends ReactiveMongoRepository<OrderEvent, String> {
    Flux<OrderEvent> findByOrderId(String orderId);
    Flux<OrderEvent> findByTimestampBetween(Instant from, Instant to);
}
