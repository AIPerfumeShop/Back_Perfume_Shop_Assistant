package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.analytics.AnalyticsFilterRequest;
import com.example.spring_boot_project_api.dto.response.ai.AIAnalyticsDashboardResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIClickAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIConversationAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIMessageAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIPopularQuestionResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIRecommendationAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIUsageAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIUsageTrendResponse;
import com.example.spring_boot_project_api.service.AIAnalyticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(summary = "Get AI conversation statistics with optional date range",
            description = "Returns total conversations, unique active users, and average messages per conversation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversation analytics retrieved successfully")
    })
    @GetMapping("/conversations")
    public ResponseEntity<AIConversationAnalyticsResponse> getConversationAnalytics(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(aiAnalyticsService.getConversationAnalytics(filter));
    }

    @Operation(summary = "Get AI message statistics with optional date range",
            description = "Returns total messages, user vs AI message counts, and average messages per conversation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Message analytics retrieved successfully")
    })
    @GetMapping("/messages")
    public ResponseEntity<AIMessageAnalyticsResponse> getMessageAnalytics(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(aiAnalyticsService.getMessageAnalytics(filter));
    }

    @Operation(summary = "Get overall AI usage statistics with optional date range",
            description = "Returns aggregate counts for conversations, messages, recommendations, clicks, and unique users.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usage analytics retrieved successfully")
    })
    @GetMapping("/usage")
    public ResponseEntity<AIUsageAnalyticsResponse> getUsageAnalytics(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(aiAnalyticsService.getUsageAnalytics(filter));
    }

    @Operation(summary = "Get daily AI usage trends with optional date range",
            description = "Returns a daily breakdown of conversations, messages, recommendations, and clicks.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usage trends retrieved successfully")
    })
    @GetMapping("/trends")
    public ResponseEntity<List<AIUsageTrendResponse>> getUsageTrends(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(aiAnalyticsService.getUsageTrends(filter));
    }

    @Operation(summary = "Get top AI user questions with optional date range",
            description = "Returns the most frequently asked user messages (top 10) in the given date range.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Popular questions retrieved successfully")
    })
    @GetMapping("/popular-questions")
    public ResponseEntity<List<AIPopularQuestionResponse>> getPopularQuestions(
            @ModelAttribute AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(aiAnalyticsService.getPopularQuestions(filter));
    }
}