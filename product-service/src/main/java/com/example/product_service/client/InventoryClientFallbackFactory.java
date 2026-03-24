package com.example.product_service.client;

import com.example.product_service.exception.InventoryUnavailableException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {

    @Override
    public InventoryClient create(Throwable cause) {
        return new InventoryClient() {
            @Override
            public InventoryResponse addStock(AddStockRequest request) {
                throw new InventoryUnavailableException("Inventory service is unavailable", cause);
            }

            @Override
            public InventoryResponse getStock(Long productId) {
                throw new InventoryUnavailableException("Inventory service is unavailable", cause);
            }

            @Override
            public List<InventoryResponse> getStocks(List<Long> productIds) {
                throw new InventoryUnavailableException("Inventory service is unavailable", cause);
            }
        };
    }
}
