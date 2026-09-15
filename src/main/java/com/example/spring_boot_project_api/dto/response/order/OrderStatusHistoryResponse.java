package com.example.spring_boot_project_api.dto.response.order;

import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.OrderStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStatusHistoryResponse {
    private Long id;
    private Long orderId;
    private OrderStatus status;
    private String note;
    private LocalDateTime changedAt;
}