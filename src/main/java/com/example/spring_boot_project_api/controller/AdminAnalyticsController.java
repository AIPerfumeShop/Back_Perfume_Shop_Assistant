package com.example.spring_boot_project_api.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.spring_boot_project_api.dto.request.analytics.AnalyticsFilterRequest;
import com.example.spring_boot_project_api.dto.response.analytics.*;
import com.example.spring_boot_project_api.repository.ExpenseRepository;
import com.example.spring_boot_project_api.service.AnalyticsService;

/** Compatibility routes matching the documented /api/admin/analytics namespace. */
@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {
    private final AnalyticsService analyticsService;
    private final ExpenseRepository expenseRepository;

    public AdminAnalyticsController(AnalyticsService analyticsService, ExpenseRepository expenseRepository) {
        this.analyticsService = analyticsService;
        this.expenseRepository = expenseRepository;
    }

    @GetMapping({"/overview", "/dashboard"})
    public ResponseEntity<DashboardResponse> overview(@ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getDashboard(filter));
    }
    @GetMapping("/sales")
    public ResponseEntity<SalesAnalyticsResponse> sales(@ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getSalesAnalytics(filter));
    }
    @GetMapping("/products")
    public ResponseEntity<ProductAnalyticsResponse> products(@ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getProductAnalytics(filter));
    }
    @GetMapping("/categories")
    public ResponseEntity<CategoryAnalyticsResponse> categories(@ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getCategoryAnalytics(filter));
    }
    @GetMapping("/customers")
    public ResponseEntity<CustomerAnalyticsResponse> customers(@ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getCustomerAnalytics(filter));
    }
    @GetMapping("/expenses")
    public ResponseEntity<AnalyticsController.ExpenseAnalyticsResponse> expenses(
            @ModelAttribute AnalyticsFilterRequest filter) {
        LocalDate from = filter.getFrom() == null ? LocalDate.of(1900, 1, 1) : filter.getFrom();
        LocalDate to = filter.getTo() == null ? LocalDate.of(9999, 12, 31) : filter.getTo();
        BigDecimal total = expenseRepository.sumAmountBetween(from, to);
        var categories = expenseRepository.sumAmountByCategoryBetween(from, to).stream()
                .map(row -> new AnalyticsController.ExpenseAnalyticsResponse.CategoryTotal(row.getCategory(), row.getTotal()))
                .toList();
        return ResponseEntity.ok(new AnalyticsController.ExpenseAnalyticsResponse(total, categories));
    }
}
