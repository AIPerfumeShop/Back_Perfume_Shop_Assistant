package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.ai.AIRecommendationClickRequest;
import com.example.spring_boot_project_api.dto.request.ai.AIRecommendationRequest;
import com.example.spring_boot_project_api.dto.response.ai.AIRecommendationClickResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIRecommendationResponse;
import com.example.spring_boot_project_api.exception.UnauthorizedException;
import com.example.spring_boot_project_api.service.AIRecommendationService;
import com.example.spring_boot_project_api.util.SecurityUtils;

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

    private Long currentUserId() {
        return SecurityUtils.currentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }

    @Operation(summary = "Get AI product recommendations")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully")
    })
    @PostMapping
    public ResponseEntity<List<AIRecommendationResponse>> recommend(
            @Valid @RequestBody AIRecommendationRequest request) {
        return ResponseEntity.ok(recommendationService.recommend(currentUserId(), request));
    }

    @Operation(summary = "Get the latest products recommended in an AI conversation")
    @GetMapping("/conversations/{conversationId}/latest")
    public ResponseEntity<List<AIRecommendationResponse>> getLatestConversationRecommendations(
            @PathVariable Long conversationId) {
        return ResponseEntity.ok(recommendationService.getLatestConversationRecommendations(
                currentUserId(), conversationId));
    }

    @Operation(summary = "Track a click on an AI recommendation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Click tracked successfully")
    })
    @PostMapping("/click")
    public ResponseEntity<AIRecommendationClickResponse> trackClick(
            @Valid @RequestBody AIRecommendationClickRequest request) {
        return ResponseEntity.ok(recommendationService.trackClick(currentUserId(), request));
    }
}
