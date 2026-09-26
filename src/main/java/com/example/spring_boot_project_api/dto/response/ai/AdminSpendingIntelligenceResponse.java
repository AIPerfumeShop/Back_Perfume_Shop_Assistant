package com.example.spring_boot_project_api.dto.response.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminSpendingIntelligenceResponse {
    private LocalDate from;
    private LocalDate to;
    private BigDecimal totalRevenue;
    private BigDecimal totalExpenses;
    private BigDecimal totalProfit;
    private BigDecimal profitMargin;
    private long totalOrders;
    private BigDecimal averageOrderValue;
    private Map<String, BigDecimal> expensesByCategory;
    private String topExpenseCategory;
    private boolean aiGenerated;
    private String insight;
    private List<String> insights;
}
