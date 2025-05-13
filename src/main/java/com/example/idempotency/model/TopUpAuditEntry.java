package com.example.idempotency.model;

import java.time.Instant;

public class TopUpAuditEntry {
    private final String userId;
    private final String idempotencyKey;
    private final double amount;
    private final Instant timestamp;
    private final String status; // e.g., "NEW", "DUPLICATE", "RATE_LIMITED"

    public TopUpAuditEntry(String userId, String idempotencyKey, double amount, String status) {
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.timestamp = Instant.now();
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }


    public String getIdempotencyKey() {
        return idempotencyKey;
    }


    public double getAmount() {
        return amount;
    }


    public Instant getTimestamp() {
        return timestamp;
    }


    public String getStatus() {
        return status;
    }

}
