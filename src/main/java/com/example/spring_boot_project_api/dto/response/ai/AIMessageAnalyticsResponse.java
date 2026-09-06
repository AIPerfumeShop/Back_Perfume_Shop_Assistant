package com.example.spring_boot_project_api.dto.response.ai;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AIMessageAnalyticsResponse {
    private long totalMessages;
    private long userMessages;
    private long aiMessages;
    private double averageMessagesPerConversation;
}