package com.example.spring_boot_project_api.dto.response.ai;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AIUsageAnalyticsResponse {
    private long totalConversations;
    private long totalMessages;
    private long totalRecommendations;
    private long totalClicks;
    private long uniqueUsers;
}