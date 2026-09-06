package com.example.spring_boot_project_api.dto.response.dashboard;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BestSellerResponse {
    private String productName;
    private String brand;
    private Long quantitySold;
    private BigDecimal totalRevenue;
}