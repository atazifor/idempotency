package com.example.idempotency.model;

public record TopUpRequest(String userId, String idempotencyKey, double amount) {}
