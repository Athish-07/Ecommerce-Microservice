package com.example.product_service.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class ProductResponse {
    Long id;
    String name;
    BigDecimal price;
    Integer stock;
}
