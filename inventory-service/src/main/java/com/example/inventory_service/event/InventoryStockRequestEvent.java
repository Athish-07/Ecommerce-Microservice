package com.example.inventory_service.event;

public record InventoryStockRequestEvent(
        Long orderId,
        Long productId,
        Integer quantity
) {
}
