package com.example.idempotency.controller;

import com.example.idempotency.dto.OrderRequest;
import com.example.idempotency.dto.OrderResponse;
import com.example.idempotency.model.Order;
import com.example.idempotency.model.OrderStatus;
import com.example.idempotency.repository.OrderRepository;
import com.example.idempotency.service.OrderStateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    final static Logger logger = LoggerFactory.getLogger(OrderController.class);

    public final OrderRepository orderRepository;
    public final KafkaTemplate<String, String> kafkaTemplate;
    public final OrderStateService orderStateService;

    @PostMapping
    public Mono<ResponseEntity<OrderResponse>> createOrder(@RequestBody OrderRequest request) {

        Order order = new Order(
                null,
                request.userId(),
                request.product(),
                request.quantity(),
                request.price(),
                OrderStatus.PENDING,
                Instant.now()
        );
        return orderRepository.save(order)
                .log()
                .doOnSuccess(saved ->  sendOrderEvent(saved, "order-created"))
                .map(OrderResponse::from)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved));

    }

    private void sendOrderEvent(Order order, String eventType) {
        try {
            ObjectNode event = new ObjectMapper().createObjectNode();
            event.put("eventType", eventType);
            event.put("orderId", order.id());
            event.put("userId", order.userId());
            event.put("status", order.status().name());
            event.put("timestamp", Instant.now().toString());

            kafkaTemplate.send("order-events", order.id(), event.toString());
        } catch (Exception e) {
            logger.warn("Failed to send Kafka event: {}", e.getMessage());
        }
    }

    @PutMapping("/{orderId}/pay")
    public Mono<ResponseEntity<OrderResponse>> payOrder(@PathVariable String orderId) {
        return orderRepository.findById(orderId)
                .flatMap(orderStateService::pay)
                .flatMap(orderRepository::save)
                .doOnSuccess(saved -> sendOrderEvent(saved, "order-paid"))
                .map(OrderResponse::from)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PutMapping("/{orderId}/cancel")
    public Mono<ResponseEntity<OrderResponse>> cancelOrder(@PathVariable String orderId) {
        return orderRepository.findById(orderId)
                .flatMap(orderStateService::cancel)
                .flatMap(orderRepository::save)
                .doOnSuccess(saved -> sendOrderEvent(saved, "order-cancelled"))
                .map(OrderResponse::from)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }
}
