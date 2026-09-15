package com.example.spring_boot_project_api.dto.response.inventory;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryItemResponse {
    private Long variantId;
    private Long productId;
    private String productName;
    private String brand;
    private String sku;
    private Integer sizeMl;
    private BigDecimal price;
    private Integer stock;
    private Boolean isActive;
    private boolean lowStock;
    private String imageUrl;
}