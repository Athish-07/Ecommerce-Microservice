package com.example.inventory_service.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId) {
        super("Insufficient stock for productId: " + productId);
    }
}
