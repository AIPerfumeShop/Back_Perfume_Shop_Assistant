package com.example.spring_boot_project_api.dto.response.analytics;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.example.spring_boot_project_api.enums.OrderStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SalesAnalyticsResponse {
    private BigDecimal totalRevenue;
    private long totalOrders;
    private BigDecimal averageOrderValue;
    private Map<OrderStatus, Long> ordersByStatus;
    private List<DailySalesResponse> dailySales;
}