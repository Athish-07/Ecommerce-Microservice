package com.example.inventory_service.controller;

import com.example.inventory_service.dto.AddStockRequest;
import com.example.inventory_service.dto.ReduceStockRequest;
import com.example.inventory_service.entity.Inventory;
import com.example.inventory_service.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/add")
    public ResponseEntity<Inventory> addStock(@Valid @RequestBody AddStockRequest request) {
        Inventory inventory = inventoryService.addStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(inventory);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Inventory> getStock(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getStock(productId));
    }

    @PostMapping("/reduce")
    public ResponseEntity<String> reduceStock(@Valid @RequestBody ReduceStockRequest request) {
        inventoryService.reduceStock(request);
        return ResponseEntity.ok("Stock reduced successfully");
    }
}
