package com.example.idempotency.dto;

public record OrderRequest(
        String userId,
        String product,
        int quantity,
        double price
) {
}
