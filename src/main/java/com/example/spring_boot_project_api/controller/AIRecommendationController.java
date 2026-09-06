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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ai/recommendations")
public class AIRecommendationController {

    private final AIRecommendationService recommendationService;

    public AIRecommendationController(AIRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping
    public ResponseEntity<List<AIRecommendationResponse>> recommend(
            @RequestParam Long userId,
            @Valid @RequestBody AIRecommendationRequest request) {
        return ResponseEntity.ok(recommendationService.recommend(userId, request));
    }

    @PostMapping("/click")
    public ResponseEntity<AIRecommendationClickResponse> trackClick(
            @RequestParam Long userId,
            @Valid @RequestBody AIRecommendationClickRequest request) {
        return ResponseEntity.ok(recommendationService.trackClick(userId, request));
    }
}