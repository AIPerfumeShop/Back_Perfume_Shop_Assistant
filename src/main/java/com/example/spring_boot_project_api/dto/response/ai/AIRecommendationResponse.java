package com.example.spring_boot_project_api.dto.response.ai;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AIRecommendationResponse {
    private Long recommendationId;
    private Long productId;
    private String productName;
    private String brand;
    private BigDecimal price;
    private Double averageRate;
    private String reason;
    private Integer position;
}