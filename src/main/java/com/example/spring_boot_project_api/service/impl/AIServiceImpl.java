package com.example.spring_boot_project_api.service.impl;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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

    @Override
    @Transactional
    public AIChatResponse chat(Long userId, AIChatRequest request) {

        // 1. Find the current user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Find existing conversation or create a new one
        AIConversation conversation;

        if (request.getConversationId() == null) {

            conversation = new AIConversation();

            conversation.setUser(user);

            // Save user's name
            conversation.setUserName(user.getName());

            // Generate conversation title from first message
            conversation.setTitle(
                    generateConversationTitle(request.getMessage())
            );

            conversation = aiConversationRepository.save(conversation);

        } else {

            conversation = aiConversationRepository
                    .findById(request.getConversationId())
                    .orElseThrow(() ->
                            new RuntimeException("Conversation not found"));

            // Make sure the conversation belongs to the current user
            if (!conversation.getUser().getId().equals(userId)) {
                throw new RuntimeException(
                        "You do not have access to this conversation");
            }
        }

        // 3. Save user's message
        AIMessage userMessage =
                aiMapper.toMessageEntity(request, conversation);

        aiMessageRepository.save(userMessage);

        // 4. Send user's message to OpenRouter
        String aiText =
                openRouterService.generateResponse(request.getMessage());
        // String aiText = "Test AI response";

        // 5. Save AI response
        AIMessage aiMessage =
                aiMapper.toMessageEntity(
                        aiText,
                        MessageSender.AI,
                        conversation);

        aiMessage = aiMessageRepository.save(aiMessage);

        // 6. Build response
        AIChatResponse response = new AIChatResponse();

        response.setConversationId(conversation.getId());
        response.setMessageId(aiMessage.getId());
        response.setMessage(aiMessage.getMessage());
        response.setCreatedAt(aiMessage.getCreatedAt());
        response.setUpdatedAt(aiMessage.getUpdatedAt());

        return response;
    }
    //Get User conversation
    @Override
    @Transactional(readOnly = true)
    public List<AIConversationResponse> getUserConversations(Long userId){
        //check that user exists
        userRepository.findById(userId).orElseThrow(()->new RuntimeException("User not found"));

        //Get concersation belonging to user
        List<AIConversation> conversations = aiConversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);

        //Convert Entity -> Response DTO
        return conversations.stream()
        .map(aiMapper::toConversationResponse)
        .collect(Collectors.toList());
    }

    //Get conversation messages
    @Override
    @Transactional(readOnly = true)
    public List<AIMessageResponse> getConversationMessages(Long userId,Long conversationId){
        //find conversation
        AIConversation conversation = aiConversationRepository
                                    .findById(conversationId)
                                    .orElseThrow(()-> new RuntimeException("Conversation not found"));
        //Check Conversation Owner
        if(!conversation.getUser().getId().equals(userId)){
            throw new RuntimeException("You do not have access to this conversation");
        }
        //Get messages ordered from oldest -> newest
        List<AIMessage> messages = aiMessageRepository
                                    .findByConversationIdOrderByCreatedAtAsc(conversationId);
        //Convert Entity -> Response DTO
        return messages.stream()
                        .map(aiMapper::toMessageResponse)
                        .collect(Collectors.toList());
    }

    // Generate conversation title
    private String generateConversationTitle(String message) {

        if (message == null || message.trim().isEmpty()) {
            return "New Conversation";
        }

        String title = message.trim().replaceAll("\\s+", " ");

        if (title.length() > 50) {
            title = title.substring(0, 50).trim() + "...";
        }

        return title;
    }
}