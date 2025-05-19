package com.example.idempotency.repository;

import com.example.idempotency.model.Order;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface OrderRepository extends ReactiveMongoRepository<Order, String> {
    Flux<Order> findByUserId(String userId);
}
