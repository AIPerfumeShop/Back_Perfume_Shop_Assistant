package com.example.spring_boot_project_api.dto.request.ai;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AIRecommendationClickRequest {
    @NotNull(message = "Recommendation id is required")
    private Long recommendationId;
}