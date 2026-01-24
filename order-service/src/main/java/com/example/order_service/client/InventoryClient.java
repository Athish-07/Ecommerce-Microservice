package com.example.order_service.client;

import com.example.order_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service", configuration = FeignConfig.class)
public interface InventoryClient {

    @PostMapping("/inventory/reduce")
    String reduceStock(@RequestBody ReduceStockRequest request);

    record ReduceStockRequest(Long productId, Integer quantity) {}
}
