package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.enums.MessageSender;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ForbiddenException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.model.AIConversation;
import com.example.spring_boot_project_api.model.AIMessage;
import com.example.spring_boot_project_api.model.AIRecommendation;
import com.example.spring_boot_project_api.model.AIRecommendationClick;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.AIConversationRepository;
import com.example.spring_boot_project_api.repository.AIMessageRepository;
import com.example.spring_boot_project_api.repository.AIRecommendationClickRepository;
import com.example.spring_boot_project_api.repository.AIRecommendationRepository;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.repository.ProductRepository.ProductRatingStat;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.WishlistItemRepository;
import com.example.spring_boot_project_api.repository.WishlistRepository;
import com.example.spring_boot_project_api.repository.specification.ProductSpecification;
import com.example.spring_boot_project_api.service.AIRecommendationService;
import com.example.spring_boot_project_api.service.CustomerFragranceProfileService;
import com.example.spring_boot_project_api.service.OpenRouterService;
import com.example.spring_boot_project_api.util.FragranceSimilarityScorer;

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
    private final FragranceSimilarityScorer similarityScorer;
    private final OrderRepository orderRepository;
    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;

    public AIRecommendationServiceImpl(
            ProductRepository productRepository,
            AIRecommendationRepository recommendationRepository,
            AIRecommendationClickRepository clickRepository,
            AIConversationRepository conversationRepository,
            AIMessageRepository messageRepository,
            UserRepository userRepository,
            OpenRouterService openRouterService,
            CustomerFragranceProfileService fragranceProfileService,
            FragranceSimilarityScorer similarityScorer,
            OrderRepository orderRepository,
            WishlistRepository wishlistRepository,
            WishlistItemRepository wishlistItemRepository) {
        this.productRepository = productRepository;
        this.recommendationRepository = recommendationRepository;
        this.clickRepository = clickRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.openRouterService = openRouterService;
        this.fragranceProfileService = fragranceProfileService;
        this.similarityScorer = similarityScorer;
        this.orderRepository = orderRepository;
        this.wishlistRepository = wishlistRepository;
        this.wishlistItemRepository = wishlistItemRepository;
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
        if (conversation != null && !hasExplicitFilters(req)) {
            List<AIMessage> history = messageRepository
                    .findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            AIMessage latestCustomerMessage = history.stream()
                    .filter(message -> message.getSender() == MessageSender.USER)
                    .reduce((first, second) -> second)
                    .orElse(null);
            AISearchPreferences preferences = latestCustomerMessage == null
                    || !isRecommendationSearchMessage(latestCustomerMessage.getMessage())
                    ? AISearchPreferences.empty()
                    : openRouterService.extractSearchPreferences(List.of(latestCustomerMessage));
            AIRecommendationRequest merged = mergePreferences(req, preferences);
            if (hasAnyPreference(preferences)) {
                source = merged;
            }
        }

        AIRecommendationRequest criteria = mergeSavedPreferences(source, req.getPreferences());
        int candidateLimit = Math.max(60, Math.min(100, limit * 20));
        List<Product> candidates = queryProducts(criteria, candidateLimit);
        // Keep explicit and saved budget limits as hard constraints. If nothing
        // matches, return no results instead of silently recommending outside budget.
        Set<Long> previouslyRecommended = conversation == null
                ? Set.of()
                : recommendationRepository.findByConversationId(conversation.getId()).stream()
                        .map(recommendation -> recommendation.getProduct().getId())
                        .collect(Collectors.toSet());
        if (!previouslyRecommended.isEmpty()) {
            candidates = candidates.stream()
                    .filter(product -> !previouslyRecommended.contains(product.getId()))
                    .toList();
        }
        AIRecommendationRequest rankingCriteria = source;
        Map<Long, Product> behaviorProductsById = new java.util.LinkedHashMap<>();
        clickRepository.findTop25ByUserIdOrderByClickedAtDesc(userId).stream()
                .map(click -> click.getRecommendation().getProduct())
                .forEach(product -> behaviorProductsById.putIfAbsent(product.getId(), product));
        wishlistRepository.findByUserId(userId).ifPresent(wishlist ->
                wishlistItemRepository.findAllByWishlistId(wishlist.getId()).stream()
                        .map(item -> item.getProduct())
                        .forEach(product -> behaviorProductsById.putIfAbsent(product.getId(), product)));
        orderRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getItems)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(item -> item.getVariant() == null ? null : item.getVariant().getProduct())
                .filter(Objects::nonNull)
                .forEach(product -> behaviorProductsById.putIfAbsent(product.getId(), product));
        List<Product> behaviorProducts = List.copyOf(behaviorProductsById.values());
        List<Long> candidateIds = candidates.stream().map(Product::getId).toList();
        Map<Long, Double> avgRateById = candidateIds.isEmpty()
                ? Map.of()
                : productRepository.findRatingStats(candidateIds).stream()
                        .collect(Collectors.toMap(ProductRatingStat::getProductId,
                                ProductRatingStat::getAvgRate));
        final List<Product> finalProducts = candidates.stream()
                .sorted(Comparator.comparingInt((Product product) ->
                                recommendationScore(product, req.getPreferences(), rankingCriteria,
                                        customerProfile, behaviorProducts,
                                        avgRateById.get(product.getId())))
                        .reversed()
                        .thenComparing(Product::getId))
                .limit(limit)
                .toList();

        List<AIRecommendationResponse> responses = finalProducts.stream()
                .map(product -> toResponse(product, avgRateById.get(product.getId()), null, 0))
                .toList();

        int position = 0;
        for (Product product : finalProducts) {
            responses.get(position).setPosition(position + 1);
            responses.get(position).setReason(buildReason(product, criteria, req.getPreferences(),
                    customerProfile, behaviorProducts, avgRateById.get(product.getId())));
            position++;
        }

        if (conversation != null) {
            LocalDateTime batchCreatedAt = LocalDateTime.now();
            List<AIRecommendation> saved = recommendationRepository.saveAll(
                    finalProducts.stream()
                            .map(product -> {
                                AIRecommendation entity = new AIRecommendation();
                                entity.setConversation(conversation);
                                entity.setProduct(product);
                                entity.setReason(responses.get(finalProducts.indexOf(product)).getReason());
                                entity.setPosition(finalProducts.indexOf(product) + 1);
                                entity.setCreatedAt(batchCreatedAt);
                                return entity;
                            })
                            .toList());
            for (int i = 0; i < saved.size(); i++) {
                responses.get(i).setRecommendationId(saved.get(i).getId());
            }
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIRecommendationResponse> getLatestConversationRecommendations(Long userId, Long conversationId) {
        if (conversationId == null) {
            throw new BadRequestException("Conversation ID is required");
        }
        AIConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("AI conversation not found"));
        if (conversation.getUser() == null || !userId.equals(conversation.getUser().getId())) {
            throw new ForbiddenException("You cannot view recommendations from this conversation");
        }

        List<AIRecommendation> latest = recommendationRepository
                .findLatestBatchByConversationId(conversationId);
        if (latest.isEmpty()) return List.of();

        List<Long> productIds = latest.stream()
                .map(item -> item.getProduct().getId())
                .toList();
        Map<Long, Double> ratings = productRepository.findRatingStats(productIds).stream()
                .collect(Collectors.toMap(ProductRatingStat::getProductId, ProductRatingStat::getAvgRate));

        return latest.stream().map(item -> {
            Product product = item.getProduct();
            AIRecommendationResponse response = toResponse(product,
                    ratings.get(product.getId()), item.getReason(), item.getPosition() == null ? 0 : item.getPosition());
            response.setRecommendationId(item.getId());
            return response;
        }).toList();
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
        if (merged.getMinPrice() == null) merged.setMinPrice(preferences.getPriceMin());
        if (merged.getMaxPrice() == null) merged.setMaxPrice(preferences.getPriceMax());
        return merged;
    }

    private int recommendationScore(Product product, AIChatPreferences preferences,
            AIRecommendationRequest chatCriteria, CustomerFragranceProfileResponse customerProfile,
            List<Product> clickedProducts, Double averageRating) {
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

        // Reward affinity with products the customer chose to open from prior
        // recommendation lists; purchase-derived fragrance profile remains the
        // longer term preference signal.
        score += behaviorScore(product, clickedProducts);
        if (averageRating != null) score += (int) Math.round(averageRating * 2.0);
        if (chatCriteria.getSearch() != null) {
            String search = chatCriteria.getSearch().toLowerCase(Locale.ROOT);
            String searchable = (product.getName() + " " + brand + " " + family + " " + notes)
                    .toLowerCase(Locale.ROOT);
            if (searchable.contains(search)) score += 12;
        }
        return score;
    }

    private int behaviorScore(Product candidate, List<Product> clickedProducts) {
        return clickedProducts.stream()
                .filter(clicked -> !Objects.equals(clicked.getId(), candidate.getId()))
                .mapToInt(clicked -> similarityScorer.score(candidate, clicked) / 10)
                .max()
                .orElse(0);
    }

    private String buildReason(Product product, AIRecommendationRequest request,
            AIChatPreferences preferences, CustomerFragranceProfileResponse profile,
            List<Product> clickedProducts, Double averageRating) {
        List<String> reasons = new java.util.ArrayList<>();
        var fragrance = product.getFragranceProfile();
        String family = fragrance == null ? null : fragrance.getFragranceFamily();
        if (family != null && preferences != null && preferences.getFamilies() != null
                && preferences.getFamilies().stream().filter(Objects::nonNull)
                        .anyMatch(value -> family.toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT)))) {
            reasons.add("matches your " + family.toLowerCase(Locale.ROOT) + " preference");
        }
        if (fragrance != null && family != null) {
            String normalizedFamily = family.toLowerCase(Locale.ROOT);
            if (normalizedFamily.contains("floral") && profile.getFloral() != null && profile.getFloral() >= 55)
                reasons.add("fits your floral scent profile");
            else if (normalizedFamily.contains("fresh") && profile.getFresh() != null && profile.getFresh() >= 55)
                reasons.add("fits your fresh scent profile");
            else if (normalizedFamily.contains("woody") && profile.getWoody() != null && profile.getWoody() >= 55)
                reasons.add("fits your woody scent profile");
            else if (normalizedFamily.contains("sweet") && profile.getSweetness() != null && profile.getSweetness() >= 55)
                reasons.add("fits your sweet scent profile");
        }
        if (request.getBrand() != null && product.getBrand() != null
                && product.getBrand().getName().equalsIgnoreCase(request.getBrand()))
            reasons.add("matches the brand you requested");
        if (request.getMinPrice() != null || request.getMaxPrice() != null)
            reasons.add("is within your requested budget");
        if (behaviorScore(product, clickedProducts) > 0)
            reasons.add("is similar to products you explored");
        if (averageRating != null && averageRating >= 4.0)
            reasons.add("is highly rated by customers");
        return reasons.isEmpty() ? "Selected from in-stock products that best match your preferences."
                : "Recommended because it " + String.join(" and ", reasons) + ".";
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

    private boolean isRecommendationSearchMessage(String message) {
        if (message == null || message.isBlank()) return false;
        String text = message.toLowerCase(Locale.ROOT);
        return text.contains("recommend") || text.contains("suggest")
                || text.contains("find my perfume") || text.contains("find me a perfume")
                || text.contains("perfume for") || text.contains("fragrance for")
                || text.contains("looking for a perfume") || text.contains("want a perfume")
                || text.contains("something floral") || text.contains("something fresh")
                || text.contains("something sweet") || text.contains("within my budget");
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
        filter.setMinRate(request.getMinRate());
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
                .filter(variant -> Boolean.TRUE.equals(variant.getIsActive()))
                .filter(variant -> variant.getStock() != null && variant.getStock() > 0)
                .map(ProductVariant::getPrice)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
    }

}
