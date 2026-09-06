package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.analytics.AnalyticsFilterRequest;
import com.example.spring_boot_project_api.dto.response.ai.AIAnalyticsDashboardResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIClickAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIRecommendationAnalyticsResponse;

public interface AIAnalyticsService {
    AIRecommendationAnalyticsResponse getRecommendationAnalytics(AnalyticsFilterRequest filter);

    AIClickAnalyticsResponse getClickAnalytics(AnalyticsFilterRequest filter);

    AIAnalyticsDashboardResponse getDashboard(AnalyticsFilterRequest filter);
}