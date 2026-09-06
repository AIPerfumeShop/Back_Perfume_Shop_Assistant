package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.analytics.AnalyticsFilterRequest;
import com.example.spring_boot_project_api.dto.response.ai.AIAnalyticsDashboardResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIClickAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIRecommendationAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AITopProductResponse;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.repository.AIRecommendationClickRepository;
import com.example.spring_boot_project_api.repository.AIRecommendationClickRepository.ProductClickStat;
import com.example.spring_boot_project_api.repository.AIRecommendationRepository;
import com.example.spring_boot_project_api.repository.AIRecommendationRepository.ProductRecommendationStat;
import com.example.spring_boot_project_api.service.AIAnalyticsService;

@Service
@Transactional
public class AIAnalyticsServiceImpl implements AIAnalyticsService {

    private static final int TOP_LIMIT = 10;

    private final AIRecommendationRepository recommendationRepository;
    private final AIRecommendationClickRepository clickRepository;

    public AIAnalyticsServiceImpl(
            AIRecommendationRepository recommendationRepository,
            AIRecommendationClickRepository clickRepository) {
        this.recommendationRepository = recommendationRepository;
        this.clickRepository = clickRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AIRecommendationAnalyticsResponse getRecommendationAnalytics(AnalyticsFilterRequest filter) {
        LocalDateTime start = resolveStart(filter);
        LocalDateTime end = resolveEnd(filter);

        List<ProductRecommendationStat> stats = recommendationRepository.findTopRecommendedProducts(
                start, end, PageRequest.of(0, TOP_LIMIT));

        AIRecommendationAnalyticsResponse response = new AIRecommendationAnalyticsResponse();
        response.setTotalRecommendations(recommendationRepository.countRecommendationsBetween(start, end));
        response.setTotalConversations(recommendationRepository.countDistinctConversationsBetween(start, end));
        response.setUniqueProducts(recommendationRepository.countDistinctProductsBetween(start, end));
        response.setTopRecommendedProducts(stats.stream()
                .map(this::toTopProductResponse)
                .toList());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public AIClickAnalyticsResponse getClickAnalytics(AnalyticsFilterRequest filter) {
        LocalDateTime start = resolveStart(filter);
        LocalDateTime end = resolveEnd(filter);

        List<ProductClickStat> stats = clickRepository.findTopClickedProducts(
                start, end, PageRequest.of(0, TOP_LIMIT));

        AIClickAnalyticsResponse response = new AIClickAnalyticsResponse();
        response.setTotalClicks(clickRepository.countClicksBetween(start, end));
        response.setUniqueUsers(clickRepository.countDistinctUsersBetween(start, end));
        response.setTopClickedProducts(stats.stream()
                .map(this::toTopProductResponse)
                .toList());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public AIAnalyticsDashboardResponse getDashboard(AnalyticsFilterRequest filter) {
        LocalDateTime start = resolveStart(filter);
        LocalDateTime end = resolveEnd(filter);

        List<ProductRecommendationStat> recStats = recommendationRepository.findTopRecommendedProducts(
                start, end, PageRequest.of(0, TOP_LIMIT));
        List<ProductClickStat> clickStats = clickRepository.findTopClickedProducts(
                start, end, PageRequest.of(0, TOP_LIMIT));

        AIAnalyticsDashboardResponse response = new AIAnalyticsDashboardResponse();
        response.setTotalRecommendations(recommendationRepository.countRecommendationsBetween(start, end));
        response.setTotalClicks(clickRepository.countClicksBetween(start, end));
        response.setUniqueProducts(recommendationRepository.countDistinctProductsBetween(start, end));
        response.setUniqueUsers(clickRepository.countDistinctUsersBetween(start, end));
        response.setTopRecommendedProducts(recStats.stream()
                .map(this::toTopProductResponse)
                .toList());
        response.setTopClickedProducts(clickStats.stream()
                .map(this::toTopProductResponse)
                .toList());
        return response;
    }

    private AITopProductResponse toTopProductResponse(ProductRecommendationStat stat) {
        AITopProductResponse dto = new AITopProductResponse();
        dto.setProductId(stat.getProductId());
        dto.setProductName(stat.getProductName());
        dto.setBrand(stat.getBrand());
        dto.setCount(stat.getCount());
        return dto;
    }

    private AITopProductResponse toTopProductResponse(ProductClickStat stat) {
        AITopProductResponse dto = new AITopProductResponse();
        dto.setProductId(stat.getProductId());
        dto.setProductName(stat.getProductName());
        dto.setBrand(stat.getBrand());
        dto.setCount(stat.getCount());
        return dto;
    }

    private AnalyticsFilterRequest normalize(AnalyticsFilterRequest filter) {
        return filter == null ? new AnalyticsFilterRequest() : filter;
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("'from' date cannot be after 'to' date");
        }
    }

    private LocalDateTime resolveStart(AnalyticsFilterRequest filter) {
        AnalyticsFilterRequest request = normalize(filter);
        validateRange(request.getFrom(), request.getTo());
        return request.getFrom() != null
                ? request.getFrom().atStartOfDay()
                : LocalDateTime.of(1970, 1, 1, 0, 0);
    }

    private LocalDateTime resolveEnd(AnalyticsFilterRequest filter) {
        AnalyticsFilterRequest request = normalize(filter);
        return request.getTo() != null
                ? request.getTo().plusDays(1).atStartOfDay()
                : LocalDateTime.of(9999, 12, 31, 23, 59, 59);
    }
}