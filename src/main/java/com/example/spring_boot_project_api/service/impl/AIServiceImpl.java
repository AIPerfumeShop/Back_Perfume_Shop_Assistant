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
import com.example.spring_boot_project_api.dto.request.ai.AIChatPreferences;
import com.example.spring_boot_project_api.dto.request.ai.AIRecommendationRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIChatResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIConversationResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIMessageResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIRecommendationResponse;
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
import com.example.spring_boot_project_api.service.CustomerFragranceProfileService;
import com.example.spring_boot_project_api.service.OpenRouterService;
import com.example.spring_boot_project_api.service.AIRecommendationService;
import com.example.spring_boot_project_api.util.ProductCatalogBuilder;

@Service
public class AIServiceImpl implements AIService {

    private final AIConversationRepository aiConversationRepository;
    private final AIMessageRepository aiMessageRepository;
    private final UserRepository userRepository;
    private final ProductCatalogBuilder productCatalogBuilder;
    private final AIMapper aiMapper;
    private final OpenRouterService openRouterService;
    private final CustomerFragranceProfileService fragranceProfileService;
    private final AIRecommendationService recommendationService;
    private static final int MAX_HISTORY_SIZE = 30;

    public AIServiceImpl(
            AIConversationRepository aiConversationRepository,
            AIMessageRepository aiMessageRepository,
            UserRepository userRepository,
            ProductCatalogBuilder productCatalogBuilder,
            AIMapper aiMapper,
            OpenRouterService openRouterService,
            CustomerFragranceProfileService fragranceProfileService,
            AIRecommendationService recommendationService) {

        this.aiConversationRepository = aiConversationRepository;
        this.aiMessageRepository = aiMessageRepository;
        this.userRepository = userRepository;
        this.productCatalogBuilder = productCatalogBuilder;
        this.aiMapper = aiMapper;
        this.openRouterService = openRouterService;
        this.fragranceProfileService = fragranceProfileService;
        this.recommendationService = recommendationService;
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

        String catalog = productCatalogBuilder.build()
                + buildPersonalizationContext(userId, request.getPreferences())
                + buildRankedRecommendationContext(userId, conversation, request.getMessage(), request.getPreferences());
        String aiText = openRouterService.generateResponse(
                history,
                catalog);
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
        String catalog = productCatalogBuilder.build()
                + buildPersonalizationContext(userId, request.getPreferences())
                + buildRankedRecommendationContext(userId, conversation, request.getMessage(), request.getPreferences());
        openRouterService.streamGenerateResponse(history,
                catalog, token -> {
            collected.append(token);
            onToken.accept(token);
        });

        if (collected.toString().isBlank()) {
            throw new AIServiceException("No response received from OpenRouter");
        }

        AIMessage aiMessage = saveAssistantMessage(collected.toString(), conversation);

        return buildChatResponse(conversation.getId(), aiMessage);
    }

    private String buildPersonalizationContext(Long userId, AIChatPreferences preferences) {
        var profile = fragranceProfileService.getOrGenerate(userId);
        StringBuilder context = new StringBuilder("\n\nCustomer's saved scent profile (use only when making fragrance suggestions):")
                .append("\n- Style: ").append(profile.getPersonality())
                .append("\n- Sweetness: ").append(profile.getSweetness()).append("/100")
                .append("\n- Floral: ").append(profile.getFloral()).append("/100")
                .append("\n- Fresh: ").append(profile.getFresh()).append("/100")
                .append("\n- Woody: ").append(profile.getWoody()).append("/100")
                .append("\n- Intensity: ").append(profile.getIntensity());

        if (preferences != null) {
            appendList(context, "Preferred fragrance families", preferences.getFamilies());
            appendList(context, "Preferred brands", preferences.getBrands());
            appendValue(context, "Preferred gender", preferences.getGender());
            appendValue(context, "Preferred intensity", preferences.getIntensity());
            appendValue(context, "Minimum price", preferences.getPriceMin());
            appendValue(context, "Maximum price", preferences.getPriceMax());
        }
        context.append("\nFollow these saved tastes when recommending perfumes, while prioritizing the customer's current message if it conflicts.")
                .append("\nBefore suggesting products, review the conversation history. Avoid recommending products already suggested in this conversation unless the customer asks about that specific product or asks to see it again.")
                .append("\nWhen asked for another recommendation, choose different in-stock products from the catalog that best match the customer's current request and saved tastes. If no suitable new option exists, say so honestly and ask whether they want to broaden their preferences; do not repeat an old recommendation just to fill the answer.")
                .append("\nDo not turn unrelated messages or emotional disclosures into a sales pitch. If the customer declines recommendations or says they do not want to shop, acknowledge that and do not suggest products unless they later ask.")
                .append("\nIf the customer asks why you recommended a product, explain that product only; do not add alternative products unless requested.");
        return context.toString();
    }

    private String buildRankedRecommendationContext(Long userId, AIConversation conversation,
            String message, AIChatPreferences preferences) {
        if (!isRecommendationRequest(message)) return "";

        AIRecommendationRequest request = new AIRecommendationRequest();
        request.setConversationId(conversation.getId());
        request.setLimit(3);
        request.setPreferences(preferences);
        List<AIRecommendationResponse> ranked = recommendationService.recommend(userId, request);

        StringBuilder context = new StringBuilder("\n\nSYSTEM-GENERATED RANKED RECOMMENDATIONS FOR THIS REQUEST:")
                .append("\nThe backend selected these in-stock products using catalog filters, the current request, the customer's fragrance profile, and prior behavior.")
                .append("\nOnly recommend products from this ranked list for this request. Do not invent or substitute catalog items.")
                .append("\nUse each item's supplied reason to explain the match.");
        if (ranked.isEmpty()) {
            return context.append("\nNo new matching products are available. Explain that clearly and ask which constraint the customer would like to change; never repeat previously recommended products.")
                    .toString();
        }
        for (AIRecommendationResponse item : ranked) {
            context.append("\n- ").append(item.getProductName())
                    .append(" (brand: ").append(item.getBrand())
                    .append(", from $").append(item.getPrice())
                    .append(", rating: ").append(item.getAverageRate() != null ? item.getAverageRate() : "not rated")
                    .append("): ").append(item.getReason());
        }
        return context.toString();
    }

    private boolean isRecommendationRequest(String message) {
        if (message == null || message.isBlank()) return false;
        String text = message.toLowerCase(java.util.Locale.ROOT);
        if (containsAny(text, "don't recommend", "do not recommend", "no recommendations",
                "don't want to shop", "do not want to shop", "not shopping",
                "don't want a recommendation", "do not want a recommendation", "not looking for a perfume",
                "why did you recommend", "why recommend", "why this perfume")) return false;
        return containsAny(text, "recommend", "recommendation", "suggest a perfume", "suggest me",
                "find my perfume", "find me a perfume", "perfume for", "fragrance for",
                "what perfume", "which perfume", "help me choose a perfume", "what should i buy",
                "what would you recommend", "what do you recommend", "what suits me", "another perfume");
    }

    private boolean containsAny(String text, String... phrases) {
        for (String phrase : phrases) {
            if (text.contains(phrase)) return true;
        }
        return false;
    }

    private void appendList(StringBuilder context, String label, List<String> values) {
        if (values != null && !values.isEmpty()) {
            context.append("\n- ").append(label).append(": ").append(String.join(", ", values));
        }
    }

    private void appendValue(StringBuilder context, String label, Object value) {
        if (value != null && !value.toString().isBlank()) {
            context.append("\n- ").append(label).append(": ").append(value);
        }
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
