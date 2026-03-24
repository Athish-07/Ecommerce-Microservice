package com.example.inventory_service.dto;

import com.example.inventory_service.entity.Inventory;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InventoryResponse {
    Long id;
    Long productId;
    Integer stock;

    public static InventoryResponse from(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProductId())
                .stock(inventory.getStock())
                .build();
    }
}
