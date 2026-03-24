package com.example.inventory_service.service;

import com.example.inventory_service.dto.AddStockRequest;
import com.example.inventory_service.dto.ReduceStockRequest;
import com.example.inventory_service.entity.Inventory;
import com.example.inventory_service.exception.InsufficientStockException;
import com.example.inventory_service.exception.InventoryNotFoundException;
import com.example.inventory_service.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void addStockShouldIncreaseExistingInventory() {
        AddStockRequest request = new AddStockRequest();
        request.setProductId(5L);
        request.setStock(3);

        Inventory inventory = Inventory.builder()
                .id(1L)
                .productId(5L)
                .stock(7)
                .build();

        when(inventoryRepository.findByProductIdForUpdate(5L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Inventory updated = inventoryService.addStock(request);

        assertThat(updated.getStock()).isEqualTo(10);
    }

    @Test
    void reduceStockShouldRejectWhenInventoryIsTooLow() {
        ReduceStockRequest request = new ReduceStockRequest();
        request.setProductId(5L);
        request.setQuantity(8);

        Inventory inventory = Inventory.builder()
                .id(1L)
                .productId(5L)
                .stock(4)
                .build();

        when(inventoryRepository.findByProductIdForUpdate(5L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.reduceStock(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("5");
    }

    @Test
    void getStockShouldRejectWhenProductInventoryDoesNotExist() {
        when(inventoryRepository.findByProductId(12L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getStock(12L))
                .isInstanceOf(InventoryNotFoundException.class)
                .hasMessageContaining("12");
    }
}
