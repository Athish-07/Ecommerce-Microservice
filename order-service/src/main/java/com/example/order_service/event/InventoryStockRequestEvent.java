package com.example.order_service.event;

public record InventoryStockRequestEvent(
        Long orderId,
        Long productId,
        Integer quantity
) {
}
