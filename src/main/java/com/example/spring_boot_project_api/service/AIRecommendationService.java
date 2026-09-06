package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.ai.AIRecommendationClickRequest;
import com.example.spring_boot_project_api.dto.request.ai.AIRecommendationRequest;
import com.example.spring_boot_project_api.dto.response.ai.AIRecommendationClickResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIRecommendationResponse;

public interface AIRecommendationService {
    List<AIRecommendationResponse> recommend(Long userId, AIRecommendationRequest request);

    AIRecommendationClickResponse trackClick(Long userId, AIRecommendationClickRequest request);
}