package com.example.spring_boot_project_api.dto.request.ai;

import java.math.BigDecimal;

import com.example.spring_boot_project_api.enums.Gender;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AIRecommendationRequest {
    private Long conversationId;
    @Size(max = 200, message = "Search keyword must be under 200 characters")
    private String search;
    private Long categoryId;
    private String brand;
    private Gender gender;
    private String fragranceFamily;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    @Min(value = 1, message = "Minimum rating must be between 1 and 5")
    @Max(value = 5, message = "Minimum rating must be between 1 and 5")
    private Integer minRate;
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 20, message = "Limit must be at most 20")
    private Integer limit = 5;
}