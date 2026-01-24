package com.example.inventory_service.service;

import com.example.inventory_service.dto.AddStockRequest;
import com.example.inventory_service.dto.ReduceStockRequest;
import com.example.inventory_service.entity.Inventory;
import com.example.inventory_service.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public Inventory addStock(AddStockRequest request) {

        Inventory inventory = inventoryRepository
                .findByProductId(request.getProductId())
                .orElse(
                        Inventory.builder()
                                .productId(request.getProductId())
                                .stock(0)
                                .build()
                );

        inventory.setStock(inventory.getStock() + request.getStock());
        return inventoryRepository.save(inventory);
    }

    public Inventory getStock(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));
    }

    public void reduceStock(ReduceStockRequest request) {

        Inventory inventory = inventoryRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        if (inventory.getStock() < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock");
        }

        inventory.setStock(inventory.getStock() - request.getQuantity());
        inventoryRepository.save(inventory);
    }
}
