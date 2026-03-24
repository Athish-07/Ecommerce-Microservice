package com.example.order_service.client;

import com.example.order_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient(
        name = "product-service",
        configuration = FeignConfig.class,
        fallbackFactory = ProductClientFallbackFactory.class
)
public interface ProductClient {

    @GetMapping("/products/{productId}")
    ProductResponse getProduct(@PathVariable Long productId);

    record ProductResponse(Long id, String name, BigDecimal price, Integer stock) {}
}
