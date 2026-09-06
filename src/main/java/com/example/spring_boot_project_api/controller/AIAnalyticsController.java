package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.analytics.AnalyticsFilterRequest;
import com.example.spring_boot_project_api.dto.response.ai.AIAnalyticsDashboardResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIClickAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIRecommendationAnalyticsResponse;
import com.example.spring_boot_project_api.service.AIAnalyticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/ai/analytics")
public class AIAnalyticsController {

    private final AIAnalyticsService aiAnalyticsService;

    public AIAnalyticsController(AIAnalyticsService aiAnalyticsService) {
        this.aiAnalyticsService = aiAnalyticsService;
    }

    @Operation(summary = "Get AI recommendation analytics with optional date range")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recommendation analytics retrieved successfully")
    })
    @GetMapping("/recommendations")
    public ResponseEntity<AIRecommendationAnalyticsResponse> getRecommendationAnalytics(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(aiAnalyticsService.getRecommendationAnalytics(filter));
    }

    @Operation(summary = "Get AI recommendation click analytics with optional date range")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Click analytics retrieved successfully")
    })
    @GetMapping("/clicks")
    public ResponseEntity<AIClickAnalyticsResponse> getClickAnalytics(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(aiAnalyticsService.getClickAnalytics(filter));
    }

    @Operation(summary = "Get combined AI analytics dashboard with optional date range")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI analytics dashboard retrieved successfully")
    })
    @GetMapping("/dashboard")
    public ResponseEntity<AIAnalyticsDashboardResponse> getDashboard(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(aiAnalyticsService.getDashboard(filter));
    }
}