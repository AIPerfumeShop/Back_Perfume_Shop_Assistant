package com.example.spring_boot_project_api.dto.response.product;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductVariantResponse {
    private Long id;
    private Long productId;
    private String sku;
    private Integer sizeMl;
    private BigDecimal price;
    private Integer stock;
    private Boolean isActive;
}
