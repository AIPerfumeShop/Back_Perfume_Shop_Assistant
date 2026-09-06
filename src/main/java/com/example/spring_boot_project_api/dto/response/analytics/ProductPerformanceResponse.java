package com.example.spring_boot_project_api.dto.response.analytics;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductPerformanceResponse {
    private String productName;
    private String brand;
    private Long quantitySold;
    private BigDecimal totalRevenue;
}