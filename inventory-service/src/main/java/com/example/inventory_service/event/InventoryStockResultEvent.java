package com.example.inventory_service.event;

public record InventoryStockResultEvent(
        Long orderId,
        Long productId,
        boolean success,
        String message
) {
}
