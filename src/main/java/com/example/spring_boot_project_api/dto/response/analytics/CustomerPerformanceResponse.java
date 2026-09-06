package com.example.spring_boot_project_api.dto.response.analytics;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerPerformanceResponse {
    private String customerName;
    private String email;
    private Long orderCount;
    private BigDecimal totalSpent;
}