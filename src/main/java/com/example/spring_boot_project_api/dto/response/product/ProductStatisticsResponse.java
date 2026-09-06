package com.example.spring_boot_project_api.dto.response.product;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductStatisticsResponse {
    private Long totalProductsSold;
    private BigDecimal totalProductRevenue;
    private long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private Double averageRating;
    private Long reviewCount;
}