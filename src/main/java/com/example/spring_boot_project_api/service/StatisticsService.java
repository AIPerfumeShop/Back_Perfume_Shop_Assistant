package com.example.spring_boot_project_api.service;

import java.util.List;
import java.util.Map;

import com.example.spring_boot_project_api.dto.request.analytics.AnalyticsFilterRequest;
import com.example.spring_boot_project_api.dto.response.analytics.BrandPerformanceResponse;
import com.example.spring_boot_project_api.dto.response.analytics.DailySalesResponse;
import com.example.spring_boot_project_api.dto.response.product.ProductStatisticsResponse;
import com.example.spring_boot_project_api.enums.OrderStatus;

public interface StatisticsService {
    ProductStatisticsResponse getOverview(AnalyticsFilterRequest filter);

    List<DailySalesResponse> getRevenueByDate(AnalyticsFilterRequest filter);

    Map<OrderStatus, Long> getOrdersByStatus(AnalyticsFilterRequest filter);

    List<BrandPerformanceResponse> getBrandSales(AnalyticsFilterRequest filter);
}