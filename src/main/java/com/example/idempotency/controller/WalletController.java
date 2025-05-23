package com.example.idempotency.controller;

import com.example.idempotency.model.TopUpAuditEntry;
import com.example.idempotency.model.TopUpRequest;
import com.example.idempotency.model.TopUpResponse;
import com.example.idempotency.repository.TopUpAuditRepository;
import com.example.idempotency.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {
    Logger logger = LoggerFactory.getLogger(WalletController.class);

    //holds user-id and timestamp of last request for rate limiting
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofSeconds(15);
    private static final Duration IDEMPOTENCY_KEY_TTL = Duration.ofSeconds(60);
    private static final int RATE_LIMIT_COUNT = 5;

    private final WalletService walletService;
    private final ReactiveStringRedisTemplate   redisTemplate; //used for rate limiting, idempotency
    private final KafkaTemplate<String, TopUpAuditEntry> kafkaTemplate;

    private final TopUpAuditRepository repository;

    @PostMapping("/topup")
    public Mono<ResponseEntity<TopUpResponse>> topUpWallet(@RequestBody TopUpRequest request) {
        return rateLimitReached(request.userId())
                .flatMap(reached -> {
                    if (reached) {
                        logger.warn("[RATE LIMIT] userId={}, blocked", request.userId());
                        auditKafka(request, "RATE_LIMITED");
                        return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                .body(new TopUpResponse("Rate limit exceeded. Try again later.", 0, false)));
                    }
                    return checkIdempotency(request.idempotencyKey())
                            .flatMap(opt -> {
                                if (opt.isPresent()) {
                                    logger.info("[DUPLICATE] userId={}, key={}, returning previous result", request.userId(), request.idempotencyKey());
                                    auditKafka(request, "DUPLICATE");
                                    return Mono.just(ResponseEntity.ok(new TopUpResponse("Duplicate request. Returning previous result.", opt.get(), true)));
                                }
                                return
                                        simulateFailure(request)
                                                .switchIfEmpty(processTopUp(request));
                            });
                });
    }

    @GetMapping("/topup/log/{userId}")
    public ResponseEntity<Flux<TopUpAuditEntry>> getRepository(@PathVariable String userId) {
        return ResponseEntity.ok(repository.findByUserIdOrderByTimestampDesc(userId));
    }

    @GetMapping("/topup/log/{userId}/recent")
    public ResponseEntity<Flux<TopUpAuditEntry>> getRecentAuditLogs(@PathVariable String userId) {
        Instant from = Instant.now().minus(24, ChronoUnit.HOURS);
        return ResponseEntity.ok(repository.findByTimestampAfter(from));
    }

    public Mono<Boolean> rateLimitReached(String userId) {
        String key = "rate:" + userId;
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    Mono<Boolean> maybeExpire = count == 1
                            ? redisTemplate.expire(key, RATE_LIMIT_WINDOW)
                            : Mono.just(Boolean.TRUE);

                    return maybeExpire.thenReturn(count > RATE_LIMIT_COUNT);
                });
    }

    public Mono<Optional<Double>> checkIdempotency(String idemKey) {
        String idempotencyKey = "idempotency:" + idemKey;
        return redisTemplate.opsForValue()
                .get(idempotencyKey)
                .map(Double::parseDouble)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());
    }

    private Mono<ResponseEntity<TopUpResponse>> processTopUp(TopUpRequest request) {
        double newBalance = walletService.topUp(request.userId(), request.amount());
        TopUpResponse response = new TopUpResponse("Top-up successful", newBalance, false);

        //add idempotency key to redis with ttl
        return redisTemplate.opsForValue()
                .set("idempotency:" + request.idempotencyKey(), String.valueOf(response.getNewBalance()), IDEMPOTENCY_KEY_TTL)
                        .then(Mono.fromRunnable(
                                () -> auditKafka(request, "NEW")
                        )).thenReturn(ResponseEntity.ok(response));
        //add to audit trail

    }

    /*simulate a random failure (50% chance)*/
    private Mono<ResponseEntity<TopUpResponse>> simulateFailure(TopUpRequest request) {
        boolean shouldFail = new Random().nextBoolean();
        if(shouldFail) {
            String simulatedTransientFailure = new RuntimeException("Simulated transient failure").getLocalizedMessage();
            logger.error("[ERROR] userId={}, key={}, message={}", request.userId(), request.idempotencyKey(), simulatedTransientFailure);
            auditKafka(request, "TRANSIENT_ERROR");
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new TopUpResponse(simulatedTransientFailure, 0, false)));
        }
        return Mono.empty();
    }

    private void auditKafka(TopUpRequest request, String eventType) {
        TopUpAuditEntry entry = new TopUpAuditEntry(request.userId(), request.idempotencyKey(), request.amount(), eventType, Instant.now());
        kafkaTemplate.send("topup-events", request.userId(), entry);
    }
}
