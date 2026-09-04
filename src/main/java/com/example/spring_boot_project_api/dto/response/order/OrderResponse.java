package com.example.spring_boot_project_api.dto.response.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.spring_boot_project_api.enums.OrderStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderResponse {
    private Long id;
    private Long userId;
    private String userName;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String shippingAddress;
    private String phone;
    private String cancelReason;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}