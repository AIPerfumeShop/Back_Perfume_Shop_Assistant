package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.ai.AIRecommendationClickRequest;
import com.example.spring_boot_project_api.dto.request.ai.AIRecommendationRequest;
import com.example.spring_boot_project_api.dto.request.product.ProductFilterRequest;
import com.example.spring_boot_project_api.dto.response.ai.AIRecommendationClickResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIRecommendationResponse;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ForbiddenException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.model.AIConversation;
import com.example.spring_boot_project_api.model.AIRecommendation;
import com.example.spring_boot_project_api.model.AIRecommendationClick;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.AIConversationRepository;
import com.example.spring_boot_project_api.repository.AIRecommendationClickRepository;
import com.example.spring_boot_project_api.repository.AIRecommendationRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.repository.ProductRepository.ProductRatingStat;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.specification.ProductSpecification;
import com.example.spring_boot_project_api.service.AIRecommendationService;
import com.example.spring_boot_project_api.service.TelegramService;

@Service
@Transactional
public class AIRecommendationServiceImpl implements AIRecommendationService {

    private final ProductRepository productRepository;
    private final AIRecommendationRepository recommendationRepository;
    private final AIRecommendationClickRepository clickRepository;
    private final AIConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final TelegramService telegramService;

    public AIRecommendationServiceImpl(
            ProductRepository productRepository,
            AIRecommendationRepository recommendationRepository,
            AIRecommendationClickRepository clickRepository,
            AIConversationRepository conversationRepository,
            UserRepository userRepository,
            TelegramService telegramService) {
        this.productRepository = productRepository;
        this.recommendationRepository = recommendationRepository;
        this.clickRepository = clickRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.telegramService = telegramService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIRecommendationResponse> recommend(Long userId, AIRecommendationRequest request) {
        final AIRecommendationRequest req = request == null ? new AIRecommendationRequest() : request;
        int limit = req.getLimit() == null ? 5 : req.getLimit();
        AIConversation conversation = resolveConversation(userId, req.getConversationId());

        ProductFilterRequest filter = toProductFilter(req, limit);
        Page<Product> products = productRepository.findAll(
                ProductSpecification.fromFilter(filter),
                filter.toPageRequest());

        List<Long> ids = products.getContent().stream()
                .map(Product::getId)
                .toList();
        Map<Long, Double> avgRateById = ids.isEmpty()
                ? Map.of()
                : productRepository.findRatingStats(ids).stream()
                        .collect(Collectors.toMap(
                                ProductRatingStat::getProductId,
                                ProductRatingStat::getAvgRate));

        List<AIRecommendationResponse> responses = products.getContent().stream()
                .map(product -> toResponse(product, avgRateById.get(product.getId()), null, 0))
                .toList();

        int position = 0;
        for (Product product : products.getContent()) {
            responses.get(position).setPosition(position + 1);
            responses.get(position).setReason(buildReason(req));
            position++;
        }

        if (conversation != null) {
            List<AIRecommendation> saved = recommendationRepository.saveAll(
                    products.getContent().stream()
                            .map(product -> {
                                AIRecommendation entity = new AIRecommendation();
                                entity.setConversation(conversation);
                                entity.setProduct(product);
                                entity.setReason(buildReason(req));
                                entity.setPosition(products.getContent().indexOf(product) + 1);
                                return entity;
                            })
                            .toList());
            for (int i = 0; i < saved.size(); i++) {
                responses.get(i).setRecommendationId(saved.get(i).getId());
            }
        }

        telegramService.sendRecommendationSummary(
                conversation != null && conversation.getUserName() != null
                        ? conversation.getUserName()
                        : "User #" + userId,
                responses.stream()
                        .limit(5)
                        .map(response -> response.getProductName()
                                + (response.getBrand() != null ? " (" + response.getBrand() + ")" : ""))
                        .toList());

        return responses;
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