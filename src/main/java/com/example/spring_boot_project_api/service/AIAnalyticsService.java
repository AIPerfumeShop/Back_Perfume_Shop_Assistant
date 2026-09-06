package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.analytics.AnalyticsFilterRequest;
import com.example.spring_boot_project_api.dto.response.ai.AIAnalyticsDashboardResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIClickAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIConversationAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIMessageAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIPopularQuestionResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIRecommendationAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIUsageAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIUsageTrendResponse;

public interface AIAnalyticsService {
    AIRecommendationAnalyticsResponse getRecommendationAnalytics(AnalyticsFilterRequest filter);

    AIClickAnalyticsResponse getClickAnalytics(AnalyticsFilterRequest filter);

    AIAnalyticsDashboardResponse getDashboard(AnalyticsFilterRequest filter);

    AIConversationAnalyticsResponse getConversationAnalytics(AnalyticsFilterRequest filter);

    AIMessageAnalyticsResponse getMessageAnalytics(AnalyticsFilterRequest filter);

    AIUsageAnalyticsResponse getUsageAnalytics(AnalyticsFilterRequest filter);

    List<AIUsageTrendResponse> getUsageTrends(AnalyticsFilterRequest filter);

    List<AIPopularQuestionResponse> getPopularQuestions(AnalyticsFilterRequest filter);
}