package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.ai.AIRecommendationClickRequest;
import com.example.spring_boot_project_api.dto.request.ai.AIRecommendationRequest;
import com.example.spring_boot_project_api.dto.response.ai.AIRecommendationClickResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIRecommendationResponse;
import com.example.spring_boot_project_api.service.AIRecommendationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ai/recommendations")
public class AIRecommendationController {

    private final AIRecommendationService recommendationService;

    public AIRecommendationController(AIRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @Operation(summary = "Get AI product recommendations")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully")
    })
    @PostMapping
    public ResponseEntity<List<AIRecommendationResponse>> recommend(
            @RequestParam Long userId,
            @Valid @RequestBody AIRecommendationRequest request) {
        return ResponseEntity.ok(recommendationService.recommend(userId, request));
    }

    @Operation(summary = "Track a click on an AI recommendation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Click tracked successfully")
    })
    @PostMapping("/click")
    public ResponseEntity<AIRecommendationClickResponse> trackClick(
            @RequestParam Long userId,
            @Valid @RequestBody AIRecommendationClickRequest request) {
        return ResponseEntity.ok(recommendationService.trackClick(userId, request));
    }
}