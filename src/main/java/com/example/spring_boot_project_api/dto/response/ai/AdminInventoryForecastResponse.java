package com.example.spring_boot_project_api.dto.response.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminInventoryForecastResponse {
    private LocalDate from;
    private LocalDate to;
    private long daysInWindow;
    private List<ForecastItem> atRiskProducts;
    private List<ForecastItem> slowMovingProducts;
    private boolean aiGenerated;
    private String insight;
    private List<String> insights;

    @Getter
    @Setter
    public static class ForecastItem {
        private Long productId;
        private Long variantId;
        private String productName;
        private String brand;
        private Integer sizeMl;
        private Integer stock;
        private long unitsSoldInWindow;
        private double dailyDemand;
        private int daysOfCover;
        private String recommendation;
    }
}