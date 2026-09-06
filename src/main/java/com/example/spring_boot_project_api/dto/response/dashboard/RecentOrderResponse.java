package com.example.spring_boot_project_api.dto.response.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.OrderStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecentOrderResponse {
    private Long id;
    private String customerName;
    private String email;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
}