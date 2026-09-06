package com.example.spring_boot_project_api.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.analytics.AnalyticsFilterRequest;
import com.example.spring_boot_project_api.dto.response.analytics.BrandPerformanceResponse;
import com.example.spring_boot_project_api.dto.response.analytics.DailySalesResponse;
import com.example.spring_boot_project_api.dto.response.product.ProductStatisticsResponse;
import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.service.StatisticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {
    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @Operation(summary = "Get product & order statistics overview")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    @GetMapping("/overview")
    public ResponseEntity<ProductStatisticsResponse> getOverview(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(statisticsService.getOverview(filter));
    }

    @Operation(summary = "Get revenue grouped by date")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Revenue by date retrieved successfully")
    })
    @GetMapping("/revenue-by-date")
    public ResponseEntity<List<DailySalesResponse>> getRevenueByDate(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(statisticsService.getRevenueByDate(filter));
    }

    @Operation(summary = "Get order counts grouped by status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders by status retrieved successfully")
    })
    @GetMapping("/orders-by-status")
    public ResponseEntity<Map<OrderStatus, Long>> getOrdersByStatus(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(statisticsService.getOrdersByStatus(filter));
    }

    @Operation(summary = "Get products sold and revenue grouped by brand")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Brand sales retrieved successfully")
    })
    @GetMapping("/brands")
    public ResponseEntity<List<BrandPerformanceResponse>> getBrandSales(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(statisticsService.getBrandSales(filter));
    }
}