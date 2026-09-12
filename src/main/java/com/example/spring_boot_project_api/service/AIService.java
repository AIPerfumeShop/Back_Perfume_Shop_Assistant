package com.example.spring_boot_project_api.service;

import java.util.List;
import java.util.function.Consumer;

import com.example.spring_boot_project_api.dto.request.ai.AIChatRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIChatResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIConversationResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIMessageResponse;

public interface AIService {
    //send a message and receive an AI response
    AIChatResponse chat(Long userId, AIChatRequest request);
    //send a message and stream the AI response token by token
    AIChatResponse streamChat(Long userId, AIChatRequest request, Consumer<String> onToken);
    //Get a page of conversations belonging to a user (newest first)
    PagedResponse<AIConversationResponse> getUserConversations(Long userId, int page, int size);

    //Get all messages belonging to a conversation
    List<AIMessageResponse> getConversationMessages(Long userId,Long conversationId);
    AIConversationResponse updateConversation(Long userId, Long conversationId, String title);

    void deleteConversation(Long userId, Long conversationId);
}
