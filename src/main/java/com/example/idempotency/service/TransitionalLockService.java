package com.example.idempotency.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TransitionalLockService {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    public Mono<Boolean> acquireLock(String orderId) {
        String key = "transition-lock:" + orderId;
        return redisTemplate.opsForValue()
                .setIfAbsent(key, "LOCKED", Duration.ofSeconds(5));//short TTL
    }

    public Mono<Void> releaseLock(String orderId) {
        return redisTemplate.delete("transition-lock:" + orderId)
                .then();
    }
}
