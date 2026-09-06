package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.analytics.AnalyticsFilterRequest;
import com.example.spring_boot_project_api.dto.response.analytics.BrandAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.analytics.CategoryAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.analytics.CustomerAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.analytics.DashboardResponse;
import com.example.spring_boot_project_api.dto.response.analytics.ProductAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.analytics.SalesAnalyticsResponse;
import com.example.spring_boot_project_api.service.AnalyticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Operation(summary = "Get revenue and order statistics with optional date range")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sales analytics retrieved successfully")
    })
    @GetMapping("/sales")
    public ResponseEntity<SalesAnalyticsResponse> getSalesAnalytics(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getSalesAnalytics(filter));
    }

    @Operation(summary = "Get product performance with optional date range")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product analytics retrieved successfully")
    })
    @GetMapping("/products")
    public ResponseEntity<ProductAnalyticsResponse> getProductAnalytics(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getProductAnalytics(filter));
    }

    @Operation(summary = "Get category performance with optional date range")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category analytics retrieved successfully")
    })
    @GetMapping("/categories")
    public ResponseEntity<CategoryAnalyticsResponse> getCategoryAnalytics(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getCategoryAnalytics(filter));
    }

    @Operation(summary = "Get brand performance with optional date range")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Brand analytics retrieved successfully")
    })
    @GetMapping("/brands")
    public ResponseEntity<BrandAnalyticsResponse> getBrandAnalytics(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getBrandAnalytics(filter));
    }

    @Operation(summary = "Get customer statistics with optional date range")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer analytics retrieved successfully")
    })
    @GetMapping("/customers")
    public ResponseEntity<CustomerAnalyticsResponse> getCustomerAnalytics(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getCustomerAnalytics(filter));
    }

    @Operation(summary = "Get combined analytics dashboard with optional date range")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analytics dashboard retrieved successfully")
    })
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getDashboard(filter));
    }
}