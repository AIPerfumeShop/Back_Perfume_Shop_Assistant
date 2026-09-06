package com.example.spring_boot_project_api.dto.response.analytics;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryAnalyticsResponse {
    private Long totalQuantitySold;
    private BigDecimal totalRevenue;
    private List<CategoryPerformanceResponse> categories;
}