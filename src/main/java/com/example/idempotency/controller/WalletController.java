package com.example.idempotency.controller;

import com.example.idempotency.model.TopUpRequest;
import com.example.idempotency.model.TopUpResponse;
import com.example.idempotency.service.WalletService;
import com.example.idempotency.store.IdempotencyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Random;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {
    Logger logger = LoggerFactory.getLogger(WalletController.class);

    private final WalletService walletService;
    private final IdempotencyStore idempotencyStore;

    public WalletController(WalletService walletService, IdempotencyStore idempotencyStore) {
        this.walletService = walletService;
        this.idempotencyStore = idempotencyStore;
    }

    @PostMapping("/topUp")
    public Mono<ResponseEntity<TopUpResponse>> topUpWallet(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                           @RequestBody TopUpRequest request) {
        // simulate a random failure (50% chance)
        Random random = new Random();
        boolean foundInKeystore = idempotencyStore.contains(idempotencyKey);
        if(!foundInKeystore && !random.nextBoolean()) {
            return Mono.error(new RuntimeException("Simulated transient failure"));
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
