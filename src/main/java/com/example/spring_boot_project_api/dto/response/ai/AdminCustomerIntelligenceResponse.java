package com.example.spring_boot_project_api.dto.response.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCustomerIntelligenceResponse {
    private LocalDate from;
    private LocalDate to;
    private long totalCustomers;
    private long activeCustomers;
    private long newCustomers;
    private long returningCustomers;
    private long repeatCustomers;
    private Double repeatPurchaseRate;
    private Double purchaseFrequency;
    private BigDecimal averageRevenuePerCustomer;
    private BigDecimal customerLifetimeValue;
    private Map<String, BigDecimal> topBrands;
    private List<FragranceFamilyItem> topFragranceFamilies;
    private List<SegmentItem> segments;
    private boolean aiGenerated;
    private String insight;
    private List<String> insights;

    @Getter
    @Setter
    public static class FragranceFamilyItem {
        private String fragranceFamily;
        private Long unitsSold;
        private BigDecimal totalRevenue;
    }

    @Getter
    @Setter
    public static class SegmentItem {
        private String segment;
        private long count;
    }
}