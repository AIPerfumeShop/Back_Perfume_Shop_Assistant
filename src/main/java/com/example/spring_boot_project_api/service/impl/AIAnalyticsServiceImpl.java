package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.analytics.AnalyticsFilterRequest;
import com.example.spring_boot_project_api.dto.response.ai.AIAnalyticsDashboardResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIClickAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIConversationAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIMessageAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIPopularQuestionResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIRecommendationAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIUsageAnalyticsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AIUsageTrendResponse;
import com.example.spring_boot_project_api.dto.response.ai.AITopProductResponse;
import com.example.spring_boot_project_api.enums.MessageSender;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.repository.AIConversationRepository;
import com.example.spring_boot_project_api.repository.AIConversationRepository.ConversationTrendStat;
import com.example.spring_boot_project_api.repository.AIMessageRepository;
import com.example.spring_boot_project_api.repository.AIMessageRepository.MessageTrendStat;
import com.example.spring_boot_project_api.repository.AIMessageRepository.PopularQuestionStat;
import com.example.spring_boot_project_api.repository.AIRecommendationClickRepository;
import com.example.spring_boot_project_api.repository.AIRecommendationClickRepository.ClickTrendStat;
import com.example.spring_boot_project_api.repository.AIRecommendationClickRepository.ProductClickStat;
import com.example.spring_boot_project_api.repository.AIRecommendationRepository;
import com.example.spring_boot_project_api.repository.AIRecommendationRepository.ProductRecommendationStat;
import com.example.spring_boot_project_api.repository.AIRecommendationRepository.RecommendationTrendStat;
import com.example.spring_boot_project_api.service.AIAnalyticsService;

@Service
@Transactional
public class AIAnalyticsServiceImpl implements AIAnalyticsService {

    private static final int TOP_LIMIT = 10;

    private final AIRecommendationRepository recommendationRepository;
    private final AIRecommendationClickRepository clickRepository;
    private final AIConversationRepository conversationRepository;
    private final AIMessageRepository messageRepository;

    public AIAnalyticsServiceImpl(
            AIRecommendationRepository recommendationRepository,
            AIRecommendationClickRepository clickRepository,
            AIConversationRepository conversationRepository,
            AIMessageRepository messageRepository) {
        this.recommendationRepository = recommendationRepository;
        this.clickRepository = clickRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
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

    @Override
    @Transactional(readOnly = true)
    public AIConversationAnalyticsResponse getConversationAnalytics(AnalyticsFilterRequest filter) {
        LocalDateTime start = resolveStart(filter);
        LocalDateTime end = resolveEnd(filter);

        long totalConversations = conversationRepository.countByCreatedAtBetween(start, end);
        long totalMessages = messageRepository.countByCreatedAtBetween(start, end);

        AIConversationAnalyticsResponse response = new AIConversationAnalyticsResponse();
        response.setTotalConversations(totalConversations);
        response.setUniqueUsers(conversationRepository.countDistinctUsersBetween(start, end));
        response.setAverageMessagesPerConversation(totalConversations > 0
                ? Math.round((totalMessages * 100.0) / totalConversations) / 100.0
                : 0.0);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public AIMessageAnalyticsResponse getMessageAnalytics(AnalyticsFilterRequest filter) {
        LocalDateTime start = resolveStart(filter);
        LocalDateTime end = resolveEnd(filter);

        long totalMessages = messageRepository.countByCreatedAtBetween(start, end);
        long userMessages = messageRepository.countMessagesBySenderBetween(start, end, MessageSender.USER);
        long aiMessages = messageRepository.countMessagesBySenderBetween(start, end, MessageSender.AI);
        long totalConversations = conversationRepository.countByCreatedAtBetween(start, end);

        AIMessageAnalyticsResponse response = new AIMessageAnalyticsResponse();
        response.setTotalMessages(totalMessages);
        response.setUserMessages(userMessages);
        response.setAiMessages(aiMessages);
        response.setAverageMessagesPerConversation(totalConversations > 0
                ? Math.round((totalMessages * 100.0) / totalConversations) / 100.0
                : 0.0);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public AIUsageAnalyticsResponse getUsageAnalytics(AnalyticsFilterRequest filter) {
        LocalDateTime start = resolveStart(filter);
        LocalDateTime end = resolveEnd(filter);

        AIUsageAnalyticsResponse response = new AIUsageAnalyticsResponse();
        response.setTotalConversations(conversationRepository.countByCreatedAtBetween(start, end));
        response.setTotalMessages(messageRepository.countByCreatedAtBetween(start, end));
        response.setTotalRecommendations(recommendationRepository.countRecommendationsBetween(start, end));
        response.setTotalClicks(clickRepository.countClicksBetween(start, end));
        response.setUniqueUsers(clickRepository.countDistinctUsersBetween(start, end));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIUsageTrendResponse> getUsageTrends(AnalyticsFilterRequest filter) {
        LocalDateTime start = resolveStart(filter);
        LocalDateTime end = resolveEnd(filter);

        Map<LocalDate, AIUsageTrendResponse> trends = new LinkedHashMap<>();

        for (ConversationTrendStat stat : conversationRepository.findConversationTrend(start, end)) {
            LocalDate date = stat.getDay().toLocalDate();
            AIUsageTrendResponse response = trends.computeIfAbsent(date, d -> newDefaultTrend(d));
            response.setConversations(stat.getCount());
        }
        for (MessageTrendStat stat : messageRepository.findMessageTrend(start, end)) {
            LocalDate date = stat.getDay().toLocalDate();
            AIUsageTrendResponse response = trends.computeIfAbsent(date, d -> newDefaultTrend(d));
            response.setMessages(stat.getCount());
        }
        for (RecommendationTrendStat stat : recommendationRepository.findRecommendationTrend(start, end)) {
            LocalDate date = stat.getDay().toLocalDate();
            AIUsageTrendResponse response = trends.computeIfAbsent(date, d -> newDefaultTrend(d));
            response.setRecommendations(stat.getCount());
        }
        for (ClickTrendStat stat : clickRepository.findClickTrend(start, end)) {
            LocalDate date = stat.getDay().toLocalDate();
            AIUsageTrendResponse response = trends.computeIfAbsent(date, d -> newDefaultTrend(d));
            response.setClicks(stat.getCount());
        }

        return new ArrayList<>(trends.values());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIPopularQuestionResponse> getPopularQuestions(AnalyticsFilterRequest filter) {
        LocalDateTime start = resolveStart(filter);
        LocalDateTime end = resolveEnd(filter);

        return messageRepository.findPopularQuestions(
                        start, end, MessageSender.USER, PageRequest.of(0, TOP_LIMIT))
                .stream()
                .map(this::toPopularQuestionResponse)
                .toList();
    }

    private AIUsageTrendResponse newDefaultTrend(LocalDate date) {
        AIUsageTrendResponse trend = new AIUsageTrendResponse();
        trend.setDate(date);
        return trend;
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

    private AIPopularQuestionResponse toPopularQuestionResponse(PopularQuestionStat stat) {
        AIPopularQuestionResponse dto = new AIPopularQuestionResponse();
        dto.setQuestion(stat.getQuestion());
        dto.setTimesAsked(stat.getTimesAsked());
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