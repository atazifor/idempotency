package com.example.idempotency.model;

public class TopUpResponse {
    private String message;
    private double newBalance;
    private boolean duplicate;

    public TopUpResponse() {}

    public TopUpResponse(String message, double newBalance, boolean duplicate) {
        this.message = message;
        this.newBalance = newBalance;
        this.duplicate = duplicate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public double getNewBalance() {
        return newBalance;
    }

    public void setNewBalance(double newBalance) {
        this.newBalance = newBalance;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }
}
