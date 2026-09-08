package com.example.spring_boot_project_api.dto.response.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.OrderStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminOrderSummaryResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private Integer itemCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}