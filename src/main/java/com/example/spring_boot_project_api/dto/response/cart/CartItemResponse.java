package com.example.spring_boot_project_api.dto.response.cart;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemResponse {
    private Long id;
    private Long variantId;
    private String sku;
    private Integer sizeMl;
    private java.math.BigDecimal price;
    private Integer quantity;
    private java.math.BigDecimal subtotal;
    private Long productId;
    private String productName;
    private String imageUrl;
}