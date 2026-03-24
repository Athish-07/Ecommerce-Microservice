package com.example.order_service.entity;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
