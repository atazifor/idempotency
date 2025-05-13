package com.example.idempotency.controller;

import com.example.idempotency.model.TopUpRequest;
import com.example.idempotency.model.TopUpResponse;
import com.example.idempotency.service.WalletService;
import com.example.idempotency.store.IdempotencyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {
    Logger logger = LoggerFactory.getLogger(WalletController.class);

    //holds user-id and timestamp of last request for rate limiting
    private final Map<String, Instant> lastRequestMap = new ConcurrentHashMap<>();
    private final Duration rateLimitWindow = Duration.ofSeconds(3);

    private final WalletService walletService;
    private final IdempotencyStore idempotencyStore;

    public WalletController(WalletService walletService, IdempotencyStore idempotencyStore) {
        this.walletService = walletService;
        this.idempotencyStore = idempotencyStore;
    }

    @PostMapping("/topUp")
    public Mono<ResponseEntity<TopUpResponse>> topUpWallet(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                           @RequestBody TopUpRequest request) {
        //apply rate limiting
        Instant lastRequest = lastRequestMap.get(request.getUserId());
        Instant now = Instant.now();
        if(lastRequest != null && Duration.between(lastRequest, now).compareTo(rateLimitWindow) < 0) {
            logger.warn("[RATE LIMIT] userId={}, blocked", request.getUserId());
            return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new TopUpResponse("Rate limit exceeded. Try again later.", 0, false)));
        }
        lastRequestMap.put(request.getUserId(), now);

        // simulate a random failure (50% chance)
        Random random = new Random();
        boolean foundInKeystore = idempotencyStore.contains(idempotencyKey);
        if(!foundInKeystore && !random.nextBoolean()) {
            String simulatedTransientFailure = new RuntimeException("Simulated transient failure").getLocalizedMessage();
            logger.error("[ERROR] userId={}, key={}, message={}", request.getUserId(), idempotencyKey, simulatedTransientFailure);
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new TopUpResponse(simulatedTransientFailure, 0, false)));
        }


        if(foundInKeystore) {
            TopUpResponse existing = idempotencyStore.getTopUpResponse(idempotencyKey);
            logger.info("[DUPLICATE] userId={}, key={}, returning previous result", request.getUserId(), idempotencyKey);
            return Mono.just(ResponseEntity.ok(new TopUpResponse("Duplicate request. Returning previous result.", existing.getNewBalance(), true)));
        }

        logger.info("[NEW TOP-UP] userId={}, key={}, amount={}", request.getUserId(), idempotencyKey, request.getAmount());


        double newBalance = walletService.topUp(request.getUserId(), request.getAmount());
        TopUpResponse response = new TopUpResponse("Top-up successful", newBalance, false);
        idempotencyStore.store(idempotencyKey, response);
        return Mono.just(ResponseEntity.ok(response));
    }
}
