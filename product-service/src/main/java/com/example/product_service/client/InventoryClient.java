package com.example.product_service.client;

import com.example.product_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "inventory-service",
        configuration = FeignConfig.class,
        fallbackFactory = InventoryClientFallbackFactory.class
)
public interface InventoryClient {

    @PostMapping("/inventory/add")
    InventoryResponse addStock(@RequestBody AddStockRequest request);

    @GetMapping("/inventory/{productId}")
    InventoryResponse getStock(@PathVariable Long productId);

    @GetMapping("/inventory/batch")
    List<InventoryResponse> getStocks(@RequestParam List<Long> productIds);

    record AddStockRequest(Long productId, Integer stock) {}

    record InventoryResponse(Long id, Long productId, Integer stock) {}
}
