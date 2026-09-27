package com.example.spring_boot_project_api.dto.response.ai;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AIChatResponse {
    private Long conversationId;
    private Long messageId;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AIRecommendationResponse> recommendations;
    private Long editedMessageId;
    private Long userMessageId;
}
