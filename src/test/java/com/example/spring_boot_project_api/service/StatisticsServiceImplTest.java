package com.example.spring_boot_project_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.spring_boot_project_api.dto.request.analytics.AnalyticsFilterRequest;
import com.example.spring_boot_project_api.dto.response.product.ProductStatisticsResponse;
import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.repository.OrderItemRepository;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.OrderRepository.OrderStatusStat;
import com.example.spring_boot_project_api.repository.ReviewRepository;
import com.example.spring_boot_project_api.repository.ReviewRepository.ReviewRatingStat;
import com.example.spring_boot_project_api.service.impl.StatisticsServiceImpl;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    @Test
    void getOverview_computesAveragesAndRatings() {
        ReviewRatingStat rating = new ReviewRatingStat() {
            @Override
            public Double getAvgRating() {
                return 4.5;
            }

            @Override
            public Long getReviewCount() {
                return 20L;
            }
        };

        when(orderRepository.sumTotalAmountBetween(any(), any())).thenReturn(new BigDecimal("1000.00"));
        when(orderRepository.countByCreatedAtBetween(any(), any())).thenReturn(10L);
        when(orderItemRepository.sumQuantityBetween(any(), any())).thenReturn(5L);
        when(reviewRepository.findRatingSummary()).thenReturn(rating);

        ProductStatisticsResponse response = statisticsService.getOverview(new AnalyticsFilterRequest());

        assertEquals(5L, response.getTotalProductsSold());
        assertEquals(new BigDecimal("1000.00"), response.getTotalProductRevenue());
        assertEquals(10L, response.getTotalOrders());
        assertEquals(new BigDecimal("100.00"), response.getAverageOrderValue());
        assertEquals(4.5, response.getAverageRating());
        assertEquals(20L, response.getReviewCount());
    }

    @Test
    void getOverview_noOrders_averageOrderValueIsZero() {
        when(orderRepository.sumTotalAmountBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(orderRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(orderItemRepository.sumQuantityBetween(any(), any())).thenReturn(0L);
        when(reviewRepository.findRatingSummary()).thenReturn(null);

        ProductStatisticsResponse response = statisticsService.getOverview(new AnalyticsFilterRequest());

        assertEquals(BigDecimal.ZERO, response.getAverageOrderValue());
        assertEquals(0L, response.getReviewCount());
    }

    @Test
    void getOverview_invalidDateRange_throwsBadRequest() {
        AnalyticsFilterRequest filter = new AnalyticsFilterRequest();
        filter.setFrom(LocalDate.of(2026, 9, 10));
        filter.setTo(LocalDate.of(2026, 9, 1));

        assertThrows(BadRequestException.class, () -> statisticsService.getOverview(filter));
    }

    @Test
    void getOrdersByStatus_fillsDefaultsAndOverridesCounts() {
        OrderStatusStat pending = new OrderStatusStat() {
            @Override
            public OrderStatus getStatus() {
                return OrderStatus.PENDING;
            }

            @Override
            public Long getOrderCount() {
                return 2L;
            }
        };
        OrderStatusStat delivered = new OrderStatusStat() {
            @Override
            public OrderStatus getStatus() {
                return OrderStatus.DELIVERED;
            }

            @Override
            public Long getOrderCount() {
                return 1L;
            }
        };

        when(orderRepository.countOrdersByStatusInRange(any(), any()))
                .thenReturn(List.of(pending, delivered));

        Map<OrderStatus, Long> counts = statisticsService.getOrdersByStatus(new AnalyticsFilterRequest());

        assertEquals(2L, counts.get(OrderStatus.PENDING));
        assertEquals(1L, counts.get(OrderStatus.DELIVERED));
        assertEquals(0L, counts.get(OrderStatus.SHIPPED));
        assertEquals(0L, counts.get(OrderStatus.CANCELLED));
    }
}