package com.example.spring_boot_project_api.dto.response.order;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemResponse {
    private Long id;
    private Long variantId;
    private String productName;
    private String brand;
    private String variantSize;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}