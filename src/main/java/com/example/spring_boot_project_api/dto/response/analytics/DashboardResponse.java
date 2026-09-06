package com.example.spring_boot_project_api.dto.response.analytics;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardResponse {
    private SalesAnalyticsResponse sales;
    private ProductAnalyticsResponse products;
    private CategoryAnalyticsResponse categories;
    private BrandAnalyticsResponse brands;
    private CustomerAnalyticsResponse customers;
}