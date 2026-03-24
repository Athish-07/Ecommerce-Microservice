package com.example.inventory_service.controller;

import com.example.inventory_service.dto.AddStockRequest;
import com.example.inventory_service.dto.InventoryResponse;
import com.example.inventory_service.dto.ReduceStockRequest;
import com.example.inventory_service.entity.Inventory;
import com.example.inventory_service.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/add")
    public ResponseEntity<InventoryResponse> addStock(@Valid @RequestBody AddStockRequest request) {
        Inventory inventory = inventoryService.addStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(InventoryResponse.from(inventory));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getStock(@PathVariable Long productId) {
        return ResponseEntity.ok(InventoryResponse.from(inventoryService.getStock(productId)));
    }

    @GetMapping("/batch")
    public ResponseEntity<List<InventoryResponse>> getStocks(@RequestParam List<Long> productIds) {
        return ResponseEntity.ok(
                inventoryService.getStocks(productIds).stream()
                        .map(InventoryResponse::from)
                        .toList()
        );
    }

    @PostMapping("/reduce")
    public ResponseEntity<String> reduceStock(@Valid @RequestBody ReduceStockRequest request) {
        inventoryService.reduceStock(request);
        return ResponseEntity.ok("Stock reduced successfully");
    }
}
