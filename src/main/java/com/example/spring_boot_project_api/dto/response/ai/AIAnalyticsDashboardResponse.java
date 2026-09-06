package com.example.spring_boot_project_api.dto.response.ai;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AIAnalyticsDashboardResponse {
    private long totalRecommendations;
    private long totalClicks;
    private long uniqueProducts;
    private long uniqueUsers;
    private List<AITopProductResponse> topRecommendedProducts;
    private List<AITopProductResponse> topClickedProducts;
}