package com.example.product_service.service;

import com.example.product_service.client.InventoryClient;
import com.example.product_service.dto.ProductRequest;
import com.example.product_service.dto.ProductResponse;
import com.example.product_service.entity.Product;
import com.example.product_service.exception.InventoryUnavailableException;
import com.example.product_service.exception.ProductNotFoundException;
import com.example.product_service.repository.ProductRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final InventoryClient inventoryClient;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .price(request.getPrice())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Created product {} with id {}", savedProduct.getName(), savedProduct.getId());

        try {
            InventoryClient.InventoryResponse inventory = inventoryClient.addStock(
                    new InventoryClient.AddStockRequest(savedProduct.getId(), request.getStock())
            );

            log.info("Initialized inventory for productId {} with stock {}", savedProduct.getId(), inventory.stock());
            return toResponse(savedProduct, inventory.stock());
        } catch (FeignException ex) {
            log.error("Inventory setup failed for productId {}", savedProduct.getId(), ex);
            throw new InventoryUnavailableException(
                    "Inventory setup failed while creating the product. The product creation was rolled back.",
                    ex
            );
        }
    }

    public Page<ProductResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> storedProducts = productRepository.findAll(pageable);
        Map<Long, Integer> stockByProductId = resolveStocks(storedProducts.getContent());
        List<ProductResponse> products = new ArrayList<>();

        for (Product product : storedProducts.getContent()) {
            products.add(toResponse(product, stockByProductId.getOrDefault(product.getId(), 0)));
        }

        log.debug("Resolved {} products with inventory-backed stock", products.size());
        return new PageImpl<>(products, pageable, storedProducts.getTotalElements());
    }

    public ProductResponse getById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        log.debug("Resolving product {}", productId);
        return toResponse(product, resolveStock(product.getId()));
    }

    private Integer resolveStock(Long productId) {
        try {
            return inventoryClient.getStock(productId).stock();
        } catch (FeignException.NotFound ex) {
            log.warn("Inventory record missing for productId {}, defaulting stock to 0", productId);
            return 0;
        } catch (FeignException ex) {
            log.error("Inventory lookup failed for productId {}", productId, ex);
            throw new InventoryUnavailableException("Inventory lookup failed for productId: " + productId, ex);
        }
    }

    private Map<Long, Integer> resolveStocks(List<Product> products) {
        if (products.isEmpty()) {
            return Map.of();
        }

        try {
            List<Long> productIds = products.stream()
                    .map(Product::getId)
                    .toList();

            Map<Long, Integer> stockByProductId = new HashMap<>();
            inventoryClient.getStocks(productIds)
                    .forEach(inventory -> stockByProductId.put(inventory.productId(), inventory.stock()));
            return stockByProductId;
        } catch (FeignException ex) {
            log.error("Bulk inventory lookup failed for {} products", products.size(), ex);
            throw new InventoryUnavailableException("Bulk inventory lookup failed", ex);
        }
    }

    private ProductResponse toResponse(Product product, Integer stock) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .stock(stock)
                .build();
    }
}
