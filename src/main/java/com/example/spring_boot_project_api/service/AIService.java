package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.ai.AIChatRequest;
import com.example.spring_boot_project_api.dto.response.ai.AIChatResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIConversationResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIMessageResponse;

public interface AIService {
    //send a message and receive an AI response
    AIChatResponse chat(Long userId, AIChatRequest request);
    //Get all conversation belonging to a user
    List<AIConversationResponse> getUserConversations(Long userId);

    //Get all messages belonging to a conversation
    List<AIMessageResponse> getConversationMessages(Long userId,Long conversationId);
}
