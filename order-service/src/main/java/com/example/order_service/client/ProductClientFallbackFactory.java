package com.example.order_service.client;

import com.example.order_service.exception.ProductServiceUnavailableException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    @Override
    public ProductClient create(Throwable cause) {
        return productId -> {
            throw new ProductServiceUnavailableException("Product service is unavailable", cause);
        };
    }
}
