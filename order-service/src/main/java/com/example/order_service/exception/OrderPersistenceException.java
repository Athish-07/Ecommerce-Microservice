package com.example.order_service.exception;

public class OrderPersistenceException extends RuntimeException {

    public OrderPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
