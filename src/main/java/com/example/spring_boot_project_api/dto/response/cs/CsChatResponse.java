package com.example.spring_boot_project_api.dto.response.cs;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CsChatResponse {
    private Long conversationId;
    private Long messageId;
    private String message;
    private boolean handoffSuggested;
    private boolean redirectToAIAssistant;
    private LocalDateTime createdAt;
}