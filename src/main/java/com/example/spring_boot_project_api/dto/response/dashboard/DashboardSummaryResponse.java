package com.example.spring_boot_project_api.dto.response.dashboard;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.example.spring_boot_project_api.enums.OrderStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardSummaryResponse {
    private long totalCustomers;
    private long totalProducts;
    private long totalOrders;
    private BigDecimal totalRevenue;
    private Map<OrderStatus, Long> orderStatusCounts;
    private List<RecentOrderResponse> recentOrders;
    private List<BestSellerResponse> bestSellers;
}