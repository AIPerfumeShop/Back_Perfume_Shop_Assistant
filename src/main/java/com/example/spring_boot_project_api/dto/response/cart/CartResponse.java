package com.example.spring_boot_project_api.dto.response.cart;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartResponse {
    private Long id;
    private Long userId;
    private List<CartItemResponse> items = new ArrayList<>();
    private Long totalItems = 0L;
    private java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
}