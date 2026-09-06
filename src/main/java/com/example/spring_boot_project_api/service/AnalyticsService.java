package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.analytics.AnalyticsFilterRequest;
import com.example.spring_boot_project_api.dto.response.analytics.BrandAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.analytics.CategoryAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.analytics.CustomerAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.analytics.DashboardResponse;
import com.example.spring_boot_project_api.dto.response.analytics.ProductAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.analytics.SalesAnalyticsResponse;

public interface AnalyticsService {
    SalesAnalyticsResponse getSalesAnalytics(AnalyticsFilterRequest filter);

    ProductAnalyticsResponse getProductAnalytics(AnalyticsFilterRequest filter);

    CategoryAnalyticsResponse getCategoryAnalytics(AnalyticsFilterRequest filter);

    BrandAnalyticsResponse getBrandAnalytics(AnalyticsFilterRequest filter);

    CustomerAnalyticsResponse getCustomerAnalytics(AnalyticsFilterRequest filter);

    DashboardResponse getDashboard(AnalyticsFilterRequest filter);
}