package com.example.spring_boot_project_api.dto.response.ai;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AIRecommendationClickResponse {
    private Long id;
    private Long recommendationId;
    private Long productId;
    private Long userId;
    private LocalDateTime clickedAt;
}