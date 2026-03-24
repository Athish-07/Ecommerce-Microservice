package com.example.product_service.service;

import com.example.product_service.client.InventoryClient;
import com.example.product_service.dto.ProductRequest;
import com.example.product_service.dto.ProductResponse;
import com.example.product_service.entity.Product;
import com.example.product_service.exception.ProductNotFoundException;
import com.example.product_service.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private ProductService productService;

    @Test
    void createShouldPersistProductAndReturnInventoryBackedStock() {
        ProductRequest request = new ProductRequest();
        request.setName("Phone");
        request.setPrice(new BigDecimal("299.00"));
        request.setStock(5);

        Product savedProduct = Product.builder()
                .id(1L)
                .name("Phone")
                .price(new BigDecimal("299.00"))
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(inventoryClient.addStock(any())).thenReturn(new InventoryClient.InventoryResponse(1L, 1L, 5));

        ProductResponse response = productService.create(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Phone");
        assertThat(response.getPrice()).isEqualByComparingTo("299.00");
        assertThat(response.getStock()).isEqualTo(5);
    }

    @Test
    void getAllShouldResolveStockFromInventoryService() {
        Product first = Product.builder().id(1L).name("Phone").price(new BigDecimal("100.00")).build();
        Product second = Product.builder().id(2L).name("Laptop").price(new BigDecimal("500.00")).build();

        when(productRepository.findAll(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 10), 2));
        when(inventoryClient.getStocks(List.of(1L, 2L))).thenReturn(List.of(
                new InventoryClient.InventoryResponse(1L, 1L, 4),
                new InventoryClient.InventoryResponse(2L, 2L, 9)
        ));

        Page<ProductResponse> products = productService.getAll(0, 10);

        assertThat(products.getContent()).hasSize(2);
        assertThat(products.getContent().get(0).getStock()).isEqualTo(4);
        assertThat(products.getContent().get(1).getStock()).isEqualTo(9);
    }

    @Test
    void getByIdShouldFailWhenProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }
}
