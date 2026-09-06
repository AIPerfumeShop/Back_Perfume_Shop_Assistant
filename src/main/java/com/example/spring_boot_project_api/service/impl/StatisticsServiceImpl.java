package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.analytics.AnalyticsFilterRequest;
import com.example.spring_boot_project_api.dto.response.analytics.BrandPerformanceResponse;
import com.example.spring_boot_project_api.dto.response.analytics.DailySalesResponse;
import com.example.spring_boot_project_api.dto.response.product.ProductStatisticsResponse;
import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.repository.OrderItemRepository;
import com.example.spring_boot_project_api.repository.OrderItemRepository.BrandPerformanceStat;
import com.example.spring_boot_project_api.repository.OrderItemRepository.DailySalesStat;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.OrderRepository.OrderStatusStat;
import com.example.spring_boot_project_api.repository.ReviewRepository;
import com.example.spring_boot_project_api.repository.ReviewRepository.ReviewRatingStat;
import com.example.spring_boot_project_api.service.StatisticsService;

@Service
@Transactional
public class StatisticsServiceImpl implements StatisticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;

    public StatisticsServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ReviewRepository reviewRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductStatisticsResponse getOverview(AnalyticsFilterRequest filter) {
        LocalDateTime start = resolveStart(filter);
        LocalDateTime end = resolveEnd(filter);

        BigDecimal totalRevenue = orderRepository.sumTotalAmountBetween(start, end);
        long totalOrders = orderRepository.countByCreatedAtBetween(start, end);
        ReviewRatingStat rating = reviewRepository.findRatingSummary();

        ProductStatisticsResponse response = new ProductStatisticsResponse();
        response.setTotalProductsSold(orderItemRepository.sumQuantityBetween(start, end));
        response.setTotalProductRevenue(totalRevenue);
        response.setTotalOrders(totalOrders);
        response.setTotalRevenue(totalRevenue);
        response.setAverageOrderValue(totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        response.setAverageRating(rating != null ? rating.getAvgRating() : null);
        response.setReviewCount(rating != null ? rating.getReviewCount() : 0L);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailySalesResponse> getRevenueByDate(AnalyticsFilterRequest filter) {
        LocalDateTime start = resolveStart(filter);
        LocalDateTime end = resolveEnd(filter);

        return orderItemRepository.findDailySales(start, end).stream()
                .map(this::toDailySalesResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<OrderStatus, Long> getOrdersByStatus(AnalyticsFilterRequest filter) {
        LocalDateTime start = resolveStart(filter);
        LocalDateTime end = resolveEnd(filter);

        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            counts.put(status, 0L);
        }
        for (OrderStatusStat stat : orderRepository.countOrdersByStatusInRange(start, end)) {
            counts.put(stat.getStatus(), stat.getOrderCount());
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandPerformanceResponse> getBrandSales(AnalyticsFilterRequest filter) {
        LocalDateTime start = resolveStart(filter);
        LocalDateTime end = resolveEnd(filter);

        return orderItemRepository.findBrandPerformance(start, end).stream()
                .map(this::toBrandPerformanceResponse)
                .toList();
    }

    private DailySalesResponse toDailySalesResponse(DailySalesStat stat) {
        DailySalesResponse dto = new DailySalesResponse();
        dto.setDate(stat.getDay() != null ? stat.getDay().toLocalDate() : null);
        dto.setRevenue(stat.getRevenue());
        dto.setOrders(stat.getOrders() == null ? 0L : stat.getOrders());
        return dto;
    }

    private BrandPerformanceResponse toBrandPerformanceResponse(BrandPerformanceStat stat) {
        BrandPerformanceResponse dto = new BrandPerformanceResponse();
        dto.setBrandName(stat.getBrandName());
        dto.setQuantitySold(stat.getQuantitySold());
        dto.setTotalRevenue(stat.getTotalRevenue());
        return dto;
    }

    private AnalyticsFilterRequest normalize(AnalyticsFilterRequest filter) {
        return filter == null ? new AnalyticsFilterRequest() : filter;
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

    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("'from' date cannot be after 'to' date");
        }
    }
}