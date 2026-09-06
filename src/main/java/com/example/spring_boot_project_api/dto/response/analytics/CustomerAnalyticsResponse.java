package com.example.spring_boot_project_api.dto.response.analytics;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerAnalyticsResponse {
    private long totalCustomers;
    private long newCustomers;
    private BigDecimal averageOrderValuePerCustomer;
    private List<CustomerPerformanceResponse> topCustomers;
}