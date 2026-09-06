package com.example.spring_boot_project_api.dto.response.ai;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AIRecommendationAnalyticsResponse {
    private long totalRecommendations;
    private long totalConversations;
    private long uniqueProducts;
    private List<AITopProductResponse> topRecommendedProducts;
}