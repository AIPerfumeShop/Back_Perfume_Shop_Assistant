package com.example.spring_boot_project_api.dto.response.gift;

import java.math.BigDecimal;
import java.util.List;

import com.example.spring_boot_project_api.enums.GiftConfidence;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GiftRecommendationDTO {

    private Long productId;
    private String productName;
    private String brand;
    private BigDecimal price;
    private int matchScore;
    private GiftConfidence giftConfidence;
    private List<String> reasons;
}