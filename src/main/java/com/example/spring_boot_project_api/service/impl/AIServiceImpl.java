package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.ai.AIChatRequest;
import com.example.spring_boot_project_api.dto.response.ai.AIChatResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIConversationResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIMessageResponse;
import com.example.spring_boot_project_api.enums.MessageSender;
import com.example.spring_boot_project_api.mapper.AIMapper;
import com.example.spring_boot_project_api.model.AIConversation;
import com.example.spring_boot_project_api.model.AIMessage;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.AIConversationRepository;
import com.example.spring_boot_project_api.repository.AIMessageRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.AIService;
import com.example.spring_boot_project_api.service.OpenRouterService;

@Service
public class AIServiceImpl implements AIService {

    private final AIConversationRepository aiConversationRepository;
    private final AIMessageRepository aiMessageRepository;
    private final UserRepository userRepository;
    private final AIMapper aiMapper;
    private final OpenRouterService openRouterService;

    public AIServiceImpl(
            AIConversationRepository aiConversationRepository,
            AIMessageRepository aiMessageRepository,
            UserRepository userRepository,
            AIMapper aiMapper,
            OpenRouterService openRouterService) {

        this.aiConversationRepository = aiConversationRepository;
        this.aiMessageRepository = aiMessageRepository;
        this.userRepository = userRepository;
        this.aiMapper = aiMapper;
        this.openRouterService = openRouterService;
    }

    // =========================================================
    // CHAT
    // =========================================================

    @Override
    @Transactional
    public AIChatResponse chat(Long userId, AIChatRequest request) {

        // 1. Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // 2. Find existing conversation or create new conversation
        AIConversation conversation;

        if (request.getConversationId() == null) {

            // Create new conversation
            conversation = new AIConversation();

            conversation.setUser(user);
            conversation.setUserName(user.getName());

            // Generate title from first message
            conversation.setTitle(
                    generateConversationTitle(request.getMessage())
            );

            conversation = aiConversationRepository.save(conversation);

        } else {

            // Find existing conversation
            conversation = aiConversationRepository
                    .findById(request.getConversationId())
                    .orElseThrow(() ->
                            new RuntimeException("Conversation not found"));

            // Check ownership
            if (conversation.getUser() == null
                    || !conversation.getUser().getId().equals(userId)) {

                throw new RuntimeException(
                        "You do not have access to this conversation");
            }
        }

        // 3. Save user's message
        AIMessage userMessage =
                aiMapper.toMessageEntity(request, conversation);

        aiMessageRepository.save(userMessage);

        // 4. Load conversation history
        List<AIMessage> history =
                aiMessageRepository
                        .findByConversationIdOrderByCreatedAtAsc(
                                conversation.getId()
                        );

        // 5. Send conversation history to OpenRouter
        String aiText =
                openRouterService.generateResponse(history);

        // 6. Save AI response
        AIMessage aiMessage =
                aiMapper.toMessageEntity(
                        aiText,
                        MessageSender.AI,
                        conversation
                );

        aiMessage = aiMessageRepository.save(aiMessage);

        // 7. Build response
        AIChatResponse response = new AIChatResponse();

        response.setConversationId(conversation.getId());
        response.setMessageId(aiMessage.getId());
        response.setMessage(aiMessage.getMessage());
        response.setCreatedAt(aiMessage.getCreatedAt());
        response.setUpdatedAt(aiMessage.getUpdatedAt());

        return response;
    }

    // =========================================================
    // GET USER CONVERSATIONS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AIConversationResponse> getUserConversations(Long userId) {

        // Check that user exists
        userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // Get user's conversations
        List<AIConversation> conversations =
                aiConversationRepository
                        .findByUserIdOrderByUpdatedAtDesc(userId);

        // Convert Entity -> Response DTO
        return conversations.stream()
                .map(aiMapper::toConversationResponse)
                .toList();
    }

    // =========================================================
    // GET CONVERSATION MESSAGES / HISTORY
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AIMessageResponse> getConversationMessages(
            Long userId,
            Long conversationId) {

        // 1. Find conversation
        AIConversation conversation =
                aiConversationRepository
                        .findById(conversationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Conversation not found"));

        // 2. Check ownership
        if (conversation.getUser() == null
                || !conversation.getUser().getId().equals(userId)) {

            throw new RuntimeException(
                    "You do not have access to this conversation");
        }

        // 3. Get messages from oldest -> newest
        List<AIMessage> messages =
                aiMessageRepository
                        .findByConversationIdOrderByCreatedAtAsc(
                                conversationId
                        );

        // 4. Convert Entity -> Response DTO
        return messages.stream()
                .map(aiMapper::toMessageResponse)
                .toList();
    }

    // =========================================================
    // GENERATE CONVERSATION TITLE
    // =========================================================

    private String generateConversationTitle(String message) {

        if (message == null || message.trim().isEmpty()) {
            return "New Conversation";
        }

        String title = message.trim()
                .replaceAll("\\s+", " ");

        if (title.length() > 50) {
            title = title.substring(0, 50).trim() + "...";
        }

        return title;
    }
}