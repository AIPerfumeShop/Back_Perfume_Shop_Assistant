package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.analytics.AnalyticsFilterRequest;
import com.example.spring_boot_project_api.dto.response.ai.AdminAnomalyDetectionResponse;
import com.example.spring_boot_project_api.dto.response.ai.AdminBusinessBriefingResponse;
import com.example.spring_boot_project_api.dto.response.ai.AdminCustomerIntelligenceResponse;
import com.example.spring_boot_project_api.dto.response.ai.AdminCustomerPainPointsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AdminConversionIntelligenceResponse;
import com.example.spring_boot_project_api.dto.response.ai.AdminInventoryForecastResponse;
import com.example.spring_boot_project_api.dto.response.ai.AdminSpendingIntelligenceResponse;
import com.example.spring_boot_project_api.service.AdminAIAnalyticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/admin/ai-analytics")
public class AdminAIAnalyticsController {

    private final AdminAIAnalyticsService adminAIAnalyticsService;

    public AdminAIAnalyticsController(AdminAIAnalyticsService adminAIAnalyticsService) {
        this.adminAIAnalyticsService = adminAIAnalyticsService;
    }

    @Operation(summary = "AI Spending & Profit Intelligence",
            description = "Analyzes revenue, expenses, and profit and explains what the numbers mean.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Spending intelligence retrieved successfully")
    })
    @GetMapping("/spending")
    public ResponseEntity<AdminSpendingIntelligenceResponse> getSpendingIntelligence(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(adminAIAnalyticsService.getSpendingIntelligence(filter));
    }

    @Operation(summary = "AI Inventory Forecast",
            description = "Identifies products at risk of running out or becoming slow-moving.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory forecast retrieved successfully")
    })
    @GetMapping("/inventory-forecast")
    public ResponseEntity<AdminInventoryForecastResponse> getInventoryForecast(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(adminAIAnalyticsService.getInventoryForecast(filter));
    }

    @Operation(summary = "AI Anomaly Detection",
            description = "Finds unusual changes in sales, expenses, and orders.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Anomalies retrieved successfully")
    })
    @GetMapping("/anomalies")
    public ResponseEntity<AdminAnomalyDetectionResponse> getAnomalyDetection(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(adminAIAnalyticsService.getAnomalyDetection(filter));
    }

    @Operation(summary = "AI Customer Pain Points",
            description = "Analyzes reviews, support tickets, and AI questions to surface pain points.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer pain points retrieved successfully")
    })
    @GetMapping("/customer-pain-points")
    public ResponseEntity<AdminCustomerPainPointsResponse> getCustomerPainPoints(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(adminAIAnalyticsService.getCustomerPainPoints(filter));
    }

    @Operation(summary = "AI Customer Intelligence",
            description = "Analyzes customer growth, retention, repeat purchases, spend, and favorite "
                    + "brands/fragrance families.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer intelligence retrieved successfully")
    })
    @GetMapping("/customer-intelligence")
    public ResponseEntity<AdminCustomerIntelligenceResponse> getCustomerIntelligence(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(adminAIAnalyticsService.getCustomerIntelligence(filter));
    }

    @Operation(summary = "AI Conversion Intelligence",
            description = "Summarizes order-lifecycle conversion and tracked visitor, product-view, cart, "
                    + "checkout, payment, and completion events. Metrics are available when storefront "
                    + "clients submit analytics events.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversion intelligence retrieved successfully")
    })
    @GetMapping("/conversion")
    public ResponseEntity<AdminConversionIntelligenceResponse> getConversionIntelligence(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(adminAIAnalyticsService.getConversionIntelligence(filter));
    }

    @Operation(summary = "AI Business Briefing",
            description = "Combines the important insights into one simple admin summary.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Business briefing retrieved successfully")
    })
    @GetMapping("/business-briefing")
    public ResponseEntity<AdminBusinessBriefingResponse> getBusinessBriefing(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(adminAIAnalyticsService.getBusinessBriefing(filter));
    }

    @Operation(summary = "Get the latest AI business insights")
    @GetMapping("/insights")
    public ResponseEntity<AdminBusinessBriefingResponse> getInsights(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(adminAIAnalyticsService.getBusinessBriefing(filter));
    }
}
