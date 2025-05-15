package com.example.idempotency.model;

import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("topup_audit_logs")
public record TopUpAuditEntry (
    String userId,
    String idempotencyKey,
    double amount,
    String status, // e.g., "NEW", "DUPLICATE", "RATE_LIMITED"
    Instant timestamp
){}
