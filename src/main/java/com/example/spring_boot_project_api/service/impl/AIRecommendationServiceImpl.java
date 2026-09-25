package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.ai.AIRecommendationClickRequest;
import com.example.spring_boot_project_api.dto.request.ai.AIRecommendationRequest;
import com.example.spring_boot_project_api.dto.request.ai.AIChatPreferences;
import com.example.spring_boot_project_api.dto.request.ai.AISearchPreferences;
import com.example.spring_boot_project_api.dto.request.product.ProductFilterRequest;
import com.example.spring_boot_project_api.dto.response.ai.AIRecommendationClickResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIRecommendationResponse;
import com.example.spring_boot_project_api.dto.response.ai.CustomerFragranceProfileResponse;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ForbiddenException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.model.AIConversation;
import com.example.spring_boot_project_api.model.AIMessage;
import com.example.spring_boot_project_api.model.AIRecommendation;
import com.example.spring_boot_project_api.model.AIRecommendationClick;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.AIConversationRepository;
import com.example.spring_boot_project_api.repository.AIMessageRepository;
import com.example.spring_boot_project_api.repository.AIRecommendationClickRepository;
import com.example.spring_boot_project_api.repository.AIRecommendationRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.repository.ProductRepository.ProductRatingStat;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.specification.ProductSpecification;
import com.example.spring_boot_project_api.service.AIRecommendationService;
import com.example.spring_boot_project_api.service.CustomerFragranceProfileService;
import com.example.spring_boot_project_api.service.OpenRouterService;

@Service
@Transactional
public class AIRecommendationServiceImpl implements AIRecommendationService {

    private final ProductRepository productRepository;
    private final AIRecommendationRepository recommendationRepository;
    private final AIRecommendationClickRepository clickRepository;
    private final AIConversationRepository conversationRepository;
    private final AIMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final OpenRouterService openRouterService;
    private final CustomerFragranceProfileService fragranceProfileService;

    public AIRecommendationServiceImpl(
            ProductRepository productRepository,
            AIRecommendationRepository recommendationRepository,
            AIRecommendationClickRepository clickRepository,
            AIConversationRepository conversationRepository,
            AIMessageRepository messageRepository,
            UserRepository userRepository,
            OpenRouterService openRouterService,
            CustomerFragranceProfileService fragranceProfileService) {
        this.productRepository = productRepository;
        this.recommendationRepository = recommendationRepository;
        this.clickRepository = clickRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.openRouterService = openRouterService;
        this.fragranceProfileService = fragranceProfileService;
    }

    @Override
    @Transactional
    public List<AIRecommendationResponse> recommend(Long userId, AIRecommendationRequest request) {
        final AIRecommendationRequest req = request == null ? new AIRecommendationRequest() : request;
        int limit = req.getLimit() == null ? 5 : req.getLimit();
        AIConversation conversation = resolveConversation(userId, req.getConversationId());
        CustomerFragranceProfileResponse customerProfile = fragranceProfileService.getOrGenerate(userId);

        // Personalize from the chat history when the caller did not supply
        // explicit filters of its own (the frontend widget only sends a
        // limit + conversationId, so this is the path that makes "Recommended
        // for you" actually personal).
        AIRecommendationRequest source = req;
        boolean personalized = false;
        if (conversation != null && !hasExplicitFilters(req)) {
            List<AIMessage> history = messageRepository
                    .findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            AISearchPreferences preferences =
                    openRouterService.extractSearchPreferences(history);
            AIRecommendationRequest merged = mergePreferences(req, preferences);
            if (hasAnyPreference(preferences)) {
                source = merged;
                personalized = true;
            }
        }

        AIRecommendationRequest criteria = mergeSavedPreferences(source, req.getPreferences());
        int candidateLimit = Math.max(60, Math.min(100, limit * 20));
        List<Product> candidates = queryProducts(criteria, candidateLimit);
        if (candidates.isEmpty() && !sameFilters(criteria, source)) {
            candidates = queryProducts(source, candidateLimit);
        }
        if (candidates.isEmpty() && (personalized || req.getPreferences() != null)) {
            AIRecommendationRequest broadRequest = new AIRecommendationRequest();
            broadRequest.setConversationId(req.getConversationId());
            broadRequest.setLimit(req.getLimit());
            candidates = queryProducts(broadRequest, candidateLimit);
        }
        Set<Long> previouslyRecommended = conversation == null
                ? Set.of()
                : recommendationRepository.findByConversationId(conversation.getId()).stream()
                        .map(recommendation -> recommendation.getProduct().getId())
                        .collect(Collectors.toSet());
        AIRecommendationRequest rankingCriteria = source;
        final List<Product> finalProducts = candidates.stream()
                .sorted(Comparator
                        .comparing((Product product) -> previouslyRecommended.contains(product.getId()))
                        .thenComparing(Comparator.comparingInt((Product product) ->
                                preferenceScore(product, req.getPreferences(), rankingCriteria, customerProfile)).reversed())
                        .thenComparing(Product::getId))
                .limit(limit)
                .toList();
        final String displayedReason = buildReason(criteria);

        List<Long> ids = finalProducts.stream()
                .map(Product::getId)
                .toList();
        Map<Long, Double> avgRateById = ids.isEmpty()
                ? Map.of()
                : productRepository.findRatingStats(ids).stream()
                        .collect(Collectors.toMap(
                                ProductRatingStat::getProductId,
                                ProductRatingStat::getAvgRate));

        List<AIRecommendationResponse> responses = finalProducts.stream()
                .map(product -> toResponse(product, avgRateById.get(product.getId()), null, 0))
                .toList();

        int position = 0;
        for (Product product : finalProducts) {
            responses.get(position).setPosition(position + 1);
            responses.get(position).setReason(displayedReason);
            position++;
        }

        if (conversation != null) {
            List<AIRecommendation> saved = recommendationRepository.saveAll(
                    finalProducts.stream()
                            .map(product -> {
                                AIRecommendation entity = new AIRecommendation();
                                entity.setConversation(conversation);
                                entity.setProduct(product);
                                entity.setReason(displayedReason);
                                entity.setPosition(finalProducts.indexOf(product) + 1);
                                return entity;
                            })
                            .toList());
            for (int i = 0; i < saved.size(); i++) {
                responses.get(i).setRecommendationId(saved.get(i).getId());
            }
        }

        return responses;
    }

    private AIRecommendationRequest mergeSavedPreferences(AIRecommendationRequest base, AIChatPreferences preferences) {
        if (preferences == null) return base;
        AIRecommendationRequest merged = new AIRecommendationRequest();
        merged.setConversationId(base.getConversationId());
        merged.setLimit(base.getLimit());
        merged.setSearch(base.getSearch());
        merged.setCategoryId(base.getCategoryId());
        merged.setBrand(base.getBrand());
        merged.setGender(base.getGender());
        merged.setFragranceFamily(base.getFragranceFamily());
        merged.setMinPrice(base.getMinPrice());
        merged.setMaxPrice(base.getMaxPrice());
        merged.setMinRate(base.getMinRate());
        merged.setPreferences(preferences);
        if (merged.getBrand() == null && preferences.getBrands() != null && preferences.getBrands().size() == 1) {
            merged.setBrand(preferences.getBrands().get(0));
        }
        if (merged.getGender() == null && preferences.getGender() != null) {
            try { merged.setGender(com.example.spring_boot_project_api.enums.Gender.valueOf(
                    preferences.getGender().trim().toUpperCase(Locale.ROOT))); }
            catch (IllegalArgumentException ignored) { /* ranking still uses other saved preferences */ }
        }
        if (merged.getFragranceFamily() == null && preferences.getFamilies() != null
                && preferences.getFamilies().size() == 1) {
            merged.setFragranceFamily(preferences.getFamilies().get(0));
        }
        if (merged.getMinPrice() == null) merged.setMinPrice(preferences.getPriceMin());
        if (merged.getMaxPrice() == null) merged.setMaxPrice(preferences.getPriceMax());
        return merged;
    }

    private boolean sameFilters(AIRecommendationRequest first, AIRecommendationRequest second) {
        return Objects.equals(first.getSearch(), second.getSearch())
                && Objects.equals(first.getCategoryId(), second.getCategoryId())
                && Objects.equals(first.getBrand(), second.getBrand())
                && Objects.equals(first.getGender(), second.getGender())
                && Objects.equals(first.getFragranceFamily(), second.getFragranceFamily())
                && Objects.equals(first.getMinPrice(), second.getMinPrice())
                && Objects.equals(first.getMaxPrice(), second.getMaxPrice());
    }

    private int preferenceScore(Product product, AIChatPreferences preferences,
            AIRecommendationRequest chatCriteria, CustomerFragranceProfileResponse customerProfile) {
        int score = 0;
        var fragrance = product.getFragranceProfile();
        String family = fragrance == null || fragrance.getFragranceFamily() == null
                ? "" : fragrance.getFragranceFamily().toLowerCase(Locale.ROOT);
        String notes = fragrance == null || fragrance.getFragNotes() == null
                ? "" : fragrance.getFragNotes().toLowerCase(Locale.ROOT);
        String brand = product.getBrand() == null || product.getBrand().getName() == null
                ? "" : product.getBrand().getName().toLowerCase(Locale.ROOT);

        if (preferences != null) {
            if (preferences.getFamilies() != null && preferences.getFamilies().stream()
                    .filter(Objects::nonNull).map(value -> value.toLowerCase(Locale.ROOT))
                    .anyMatch(value -> family.contains(value))) score += 8;
            if (preferences.getBrands() != null && preferences.getBrands().stream()
                    .filter(Objects::nonNull).map(value -> value.toLowerCase(Locale.ROOT))
                    .anyMatch(value -> brand.contains(value))) score += 8;
            if (preferences.getGender() != null && fragrance != null && fragrance.getGender() != null
                    && fragrance.getGender().name().equalsIgnoreCase(preferences.getGender())) score += 5;
            if (preferences.getIntensity() != null && fragrance != null && fragrance.getIntensity() != null
                    && fragrance.getIntensity().name().equalsIgnoreCase(preferences.getIntensity())) score += 4;
            BigDecimal price = lowestPrice(product);
            if ((preferences.getPriceMin() == null || price.compareTo(preferences.getPriceMin()) >= 0)
                    && (preferences.getPriceMax() == null || price.compareTo(preferences.getPriceMax()) <= 0)) score += 4;
        }

        if (chatCriteria.getBrand() != null && brand.contains(chatCriteria.getBrand().toLowerCase(Locale.ROOT))) score += 8;
        if (chatCriteria.getFragranceFamily() != null
                && family.contains(chatCriteria.getFragranceFamily().toLowerCase(Locale.ROOT))) score += 8;
        if (chatCriteria.getGender() != null && fragrance != null && fragrance.getGender() == chatCriteria.getGender()) score += 5;
        score += scentProfileScore(family, notes, "floral", customerProfile.getFloral());
        score += scentProfileScore(family, notes, "fresh", customerProfile.getFresh());
        score += scentProfileScore(family, notes, "woody", customerProfile.getWoody());
        score += scentProfileScore(family, notes, "sweet", customerProfile.getSweetness());
        return score;
    }

    private int scentProfileScore(String family, String notes, String scent, Integer preference) {
        if (preference == null || preference < 55) return 0;
        boolean matches = switch (scent) {
            case "sweet" -> family.contains("sweet") || family.contains("gourmand")
                    || notes.contains("vanilla") || notes.contains("caramel") || notes.contains("honey");
            case "woody" -> family.contains("wood") || family.contains("amber")
                    || notes.contains("cedar") || notes.contains("sandalwood") || notes.contains("oud");
            default -> family.contains(scent) || notes.contains(scent);
        };
        return matches ? preference / 20 : 0;
    }

    @Override
    public AIRecommendationClickResponse trackClick(Long userId, AIRecommendationClickRequest request) {
        if (request == null || request.getRecommendationId() == null) {
            throw new BadRequestException("Recommendation id is required");
        }
        AIRecommendation recommendation = recommendationRepository.findById(
                request.getRecommendationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Recommendation not found with ID : " + request.getRecommendationId()));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID : " + userId));

        AIRecommendationClick click = new AIRecommendationClick();
        click.setRecommendation(recommendation);
        click.setUser(user);
        click.setClickedAt(java.time.LocalDateTime.now());
        AIRecommendationClick saved = clickRepository.save(click);

        AIRecommendationClickResponse response = new AIRecommendationClickResponse();
        response.setId(saved.getId());
        response.setRecommendationId(recommendation.getId());
        response.setProductId(recommendation.getProduct().getId());
        response.setUserId(userId);
        response.setClickedAt(saved.getClickedAt());
        return response;
    }

    private AIConversation resolveConversation(Long userId, Long conversationId) {
        if (conversationId == null) {
            return null;
        }
        AIConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conversation not found with ID : " + conversationId));
        if (!conversation.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Conversation does not belong to this user");
        }
        return conversation;
    }

    private List<Product> queryProducts(AIRecommendationRequest req, int limit) {
        ProductFilterRequest filter = toProductFilter(req, limit);
        Page<Product> products = productRepository.findAll(
                ProductSpecification.fromFilter(filter),
                filter.toPageRequest());
        return products.getContent();
    }

    private boolean hasExplicitFilters(AIRecommendationRequest req) {
        return (req.getSearch() != null && !req.getSearch().isBlank())
                || req.getCategoryId() != null
                || (req.getBrand() != null && !req.getBrand().isBlank())
                || req.getGender() != null
                || (req.getFragranceFamily() != null
                        && !req.getFragranceFamily().isBlank())
                || req.getMinPrice() != null
                || req.getMaxPrice() != null;
    }

    private boolean hasAnyPreference(AISearchPreferences preferences) {
        return preferences.search() != null
                || preferences.brand() != null
                || preferences.gender() != null
                || preferences.fragranceFamily() != null
                || preferences.minPrice() != null
                || preferences.maxPrice() != null;
    }

    private AIRecommendationRequest mergePreferences(
            AIRecommendationRequest req, AISearchPreferences preferences) {
        AIRecommendationRequest merged = new AIRecommendationRequest();
        merged.setConversationId(req.getConversationId());
        merged.setLimit(req.getLimit());
        merged.setSearch(req.getSearch() != null ? req.getSearch() : preferences.search());
        merged.setCategoryId(req.getCategoryId());
        merged.setBrand(req.getBrand() != null ? req.getBrand() : preferences.brand());
        merged.setGender(req.getGender() != null ? req.getGender() : preferences.gender());
        merged.setFragranceFamily(req.getFragranceFamily() != null
                ? req.getFragranceFamily() : preferences.fragranceFamily());
        merged.setMinPrice(req.getMinPrice() != null ? req.getMinPrice() : preferences.minPrice());
        merged.setMaxPrice(req.getMaxPrice() != null ? req.getMaxPrice() : preferences.maxPrice());
        merged.setMinRate(req.getMinRate());
        return merged;
    }

    private ProductFilterRequest toProductFilter(AIRecommendationRequest request, int limit) {
        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setSearch(request.getSearch());
        filter.setCategoryId(request.getCategoryId());
        filter.setBrand(request.getBrand());
        filter.setGender(request.getGender());
        filter.setFragranceFamily(request.getFragranceFamily());
        filter.setMinPrice(request.getMinPrice());
        filter.setMaxPrice(request.getMaxPrice());
        filter.setMinRate(request.getMinRate() != null ? request.getMinRate() : 4);
        filter.setInStock(Boolean.TRUE);
        filter.setPage(0);
        filter.setSize(limit);
        return filter;
    }

    private AIRecommendationResponse toResponse(Product product, Double averageRate,
                                                String reason, int position) {
        AIRecommendationResponse response = new AIRecommendationResponse();
        response.setProductId(product.getId());
        response.setProductName(product.getName());
        response.setBrand(product.getBrand() != null ? product.getBrand().getName() : null);
        response.setPrice(lowestPrice(product));
        response.setAverageRate(averageRate);
        response.setReason(reason);
        response.setPosition(position);
        return response;
    }

    private BigDecimal lowestPrice(Product product) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
    }

    private String buildReason(AIRecommendationRequest request) {
        StringBuilder reason = new StringBuilder("Recommended for you");
        if (request.getBrand() != null && !request.getBrand().isBlank()) {
            reason.append(" from ").append(request.getBrand());
        }
        if (request.getGender() != null) {
            reason.append(", ").append(request.getGender().name().toLowerCase()).append(" scent");
        }
        if (request.getFragranceFamily() != null && !request.getFragranceFamily().isBlank()) {
            reason.append(", ").append(request.getFragranceFamily()).append(" family");
        }
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            StringBuilder range = new StringBuilder();
            if (request.getMinPrice() != null) {
                range.append("$").append(request.getMinPrice());
            }
            range.append("-");
            if (request.getMaxPrice() != null) {
                range.append("$").append(request.getMaxPrice());
            }
            reason.append(", within ").append(range);
        }
        reason.append(", highly rated");
        return reason.toString();
    }
}
