package com.example.idempotency.model;

public class TopUpRequest {
    private String userId;
    private double amount;

    public TopUpRequest() {

    }
    public TopUpRequest(String userId, double amount) {
        this.userId = userId;
        this.amount = amount;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
