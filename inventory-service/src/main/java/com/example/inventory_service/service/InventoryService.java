package com.example.inventory_service.service;

import com.example.inventory_service.dto.AddStockRequest;
import com.example.inventory_service.dto.ReduceStockRequest;
import com.example.inventory_service.entity.Inventory;
import com.example.inventory_service.exception.InsufficientStockException;
import com.example.inventory_service.exception.InventoryNotFoundException;
import com.example.inventory_service.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;

    @Transactional
    public Inventory addStock(AddStockRequest request) {

        Inventory inventory = inventoryRepository
                .findByProductIdForUpdate(request.getProductId())
                .orElse(
                        Inventory.builder()
                                .productId(request.getProductId())
                                .stock(0)
                                .build()
                );

        inventory.setStock(inventory.getStock() + request.getStock());

        try {
            Inventory savedInventory = inventoryRepository.save(inventory);
            log.info("Added stock for productId {}. New stock={}", request.getProductId(), savedInventory.getStock());
            return savedInventory;
        } catch (DataIntegrityViolationException ex) {
            Inventory existingInventory = inventoryRepository.findByProductIdForUpdate(request.getProductId())
                    .orElseThrow(() -> ex);

            existingInventory.setStock(existingInventory.getStock() + request.getStock());
            Inventory savedInventory = inventoryRepository.save(existingInventory);
            log.warn("Recovered concurrent inventory creation for productId {}. New stock={}", request.getProductId(), savedInventory.getStock());
            return savedInventory;
        }
    }

    public Inventory getStock(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
        log.debug("Resolved stock for productId {} as {}", productId, inventory.getStock());
        return inventory;
    }

    public List<Inventory> getStocks(List<Long> productIds) {
        List<Inventory> inventories = inventoryRepository.findByProductIdIn(productIds);
        log.debug("Resolved {} inventory rows for {} requested product ids", inventories.size(), productIds.size());
        return inventories;
    }

    @Transactional
    public void reduceStock(ReduceStockRequest request) {

        Inventory inventory = inventoryRepository.findByProductIdForUpdate(request.getProductId())
                .orElseThrow(() -> new InventoryNotFoundException(request.getProductId()));

        if (inventory.getStock() < request.getQuantity()) {
            log.warn("Insufficient stock for productId {}. Requested={}, Available={}",
                    request.getProductId(), request.getQuantity(), inventory.getStock());
            throw new InsufficientStockException(request.getProductId());
        }

        inventory.setStock(inventory.getStock() - request.getQuantity());
        inventoryRepository.save(inventory);
        log.info("Reduced stock for productId {} by {}. Remaining={}",
                request.getProductId(), request.getQuantity(), inventory.getStock());
    }
}
