package com.example.spring_boot_project_api.dto.response.ai;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminConversionIntelligenceResponse {
    private LocalDate from;
    private LocalDate to;
    private long totalOrders;
    private long paidOrLaterOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private long pendingOrders;
    private Double paymentProgressRate;
    private Double deliveryCompletionRate;
    private Double cancellationRate;
    private long trackedVisitors;
    private long productViews;
    private long addToCartEvents;
    private long checkoutStartedEvents;
    private long paymentCompletedEvents;
    private long completedEvents;
    private Double viewToCartRate;
    private Double checkoutToPaymentRate;
    private Map<String, Long> ordersByStatus;
    private boolean eventTrackingAvailable;
    private boolean aiGenerated;
    private String insight;
    private List<String> insights;
}