package com.example.spring_boot_project_api.service.impl;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.ai.AIChatRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIChatResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIConversationResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIMessageResponse;
import com.example.spring_boot_project_api.enums.MessageSender;
import com.example.spring_boot_project_api.exception.AIServiceException;
import com.example.spring_boot_project_api.exception.ForbiddenException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
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
    private static final int MAX_HISTORY_SIZE = 30;
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

        AIConversation conversation = resolveConversation(userId, request);
        saveUserMessage(request, conversation);
        List<AIMessage> history = loadHistory(conversation.getId());

        String aiText = openRouterService.generateResponse(history);
        AIMessage aiMessage = saveAssistantMessage(aiText, conversation);

        return buildChatResponse(conversation.getId(), aiMessage);
    }

    @Override
    @Transactional
    public AIChatResponse streamChat(Long userId, AIChatRequest request, Consumer<String> onToken) {

        AIConversation conversation = resolveConversation(userId, request);
        saveUserMessage(request, conversation);
        List<AIMessage> history = loadHistory(conversation.getId());

        StringBuilder collected = new StringBuilder();
        openRouterService.streamGenerateResponse(history, token -> {
            collected.append(token);
            onToken.accept(token);
        });

        if (collected.toString().isBlank()) {
            throw new AIServiceException("No response received from OpenRouter");
        }

        AIMessage aiMessage = saveAssistantMessage(collected.toString(), conversation);

        return buildChatResponse(conversation.getId(), aiMessage);
    }

    // =========================================================
    // GET USER CONVERSATIONS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AIConversationResponse> getUserConversations(Long userId, int page, int size) {

        // Check that user exists
        userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        // Get user's conversations
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size > 0 ? Math.min(size, 50) : 20,
                Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<AIConversation> conversations =
                aiConversationRepository
                        .findByUserIdOrderByUpdatedAtDesc(userId, pageable);

        // Convert Entity -> Response DTO
        List<AIConversationResponse> content = conversations.getContent().stream()
                .map(aiMapper::toConversationResponse)
                .toList();

        return new PagedResponse<>(
                content,
                conversations.getTotalElements(),
                conversations.getTotalPages(),
                conversations.getNumber(),
                conversations.getSize());
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
                                new ResourceNotFoundException(
                                        "Conversation not found"));

        // 2. Check ownership
        if (!conversation.getUser().getId().equals(userId)) {

            throw new ForbiddenException(
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
    // CONVERSATION REUSE / PREPARATION
    // =========================================================

    private AIConversation resolveConversation(Long userId, AIChatRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getConversationId() == null) {

            AIConversation conversation = new AIConversation();
            conversation.setUser(user);
            conversation.setUserName(user.getName());
            conversation.setTitle(generateConversationTitle(request.getMessage()));
            return aiConversationRepository.save(conversation);
        }

        AIConversation conversation = aiConversationRepository
                .findById(request.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this conversation");
        }

        return conversation;
    }

    private void saveUserMessage(AIChatRequest request, AIConversation conversation) {
        AIMessage userMessage = aiMapper.toMessageEntity(request, conversation);
        aiMessageRepository.save(userMessage);
    }

    private List<AIMessage> loadHistory(Long conversationId) {
        List<AIMessage> history =
                aiMessageRepository
                        .findByConversationIdOrderByCreatedAtAsc(conversationId);
        if (history.size() > MAX_HISTORY_SIZE) {
            history = history.subList(
                    history.size() - MAX_HISTORY_SIZE,
                    history.size());
        }
        return history;
    }

    private AIMessage saveAssistantMessage(String text, AIConversation conversation) {
        AIMessage aiMessage =
                aiMapper.toMessageEntity(text, MessageSender.AI, conversation);
        aiMessage = aiMessageRepository.save(aiMessage);

        conversation.touch();
        aiConversationRepository.save(conversation);

        return aiMessage;
    }

    private AIChatResponse buildChatResponse(Long conversationId, AIMessage aiMessage) {
        AIChatResponse response = new AIChatResponse();
        response.setConversationId(conversationId);
        response.setMessageId(aiMessage.getId());
        response.setMessage(aiMessage.getMessage());
        response.setCreatedAt(aiMessage.getCreatedAt());
        response.setUpdatedAt(aiMessage.getUpdatedAt());
        return response;
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

        @Override
        @Transactional
        public AIConversationResponse updateConversation(Long userId, Long conversationId, String title) {
        AIConversation conversation = aiConversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getUser().getId().equals(userId)) {
                throw new ForbiddenException("You do not have access to this conversation");
        }

        conversation.setTitle(title);
        conversation = aiConversationRepository.save(conversation);
        return aiMapper.toConversationResponse(conversation);
        }

        @Override
        @Transactional
        public void deleteConversation(Long userId, Long conversationId) {
        AIConversation conversation = aiConversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getUser().getId().equals(userId)) {
                throw new ForbiddenException("You do not have access to this conversation");
        }

        aiConversationRepository.delete(conversation);
        }
}