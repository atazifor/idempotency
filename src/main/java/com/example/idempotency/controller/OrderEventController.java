package com.example.idempotency.controller;

import com.example.idempotency.model.OrderEvent;
import com.example.idempotency.repository.OrderEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/orders/event")
@RequiredArgsConstructor
public class OrderEventController {
    private final OrderEventRepository repository;

    @RequestMapping("/last-24h")
    public Flux<OrderEvent> getLast24hEvents() {
        Instant now = Instant.now();
        Instant cuttoff = Instant.now().minus(Duration.ofHours(24));
        return repository.findByTimestampBetween(cuttoff, now);
    }
}
