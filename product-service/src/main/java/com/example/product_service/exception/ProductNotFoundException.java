package com.example.product_service.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("Product not found for id: " + productId);
    }
}
