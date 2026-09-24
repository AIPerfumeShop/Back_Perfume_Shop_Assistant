package com.example.spring_boot_project_api.dto.response.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminBusinessBriefingResponse {
    private LocalDate from;
    private LocalDate to;
    private BigDecimal totalRevenue;
    private BigDecimal totalExpenses;
    private BigDecimal totalProfit;
    private long totalOrders;
    private long newCustomers;
    private long productsSold;
    private Map<String, BigDecimal> categoryRevenue;
    private boolean aiGenerated;
    private String insight;
    private List<String> insights;
}