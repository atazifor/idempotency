package com.example.idempotency.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WalletService {
    private final Map<String, Double> balances = new ConcurrentHashMap<>();

    public double topUp(String userId, double amount) {
        return balances.merge(userId, amount, Double::sum);
    }

    public double getBalance(String userId) {
        return balances.getOrDefault(userId, 0.0);
    }
}
