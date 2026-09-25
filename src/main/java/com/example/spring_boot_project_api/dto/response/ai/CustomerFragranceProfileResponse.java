package com.example.spring_boot_project_api.dto.response.ai;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class CustomerFragranceProfileResponse {
    private Long userId;
    private String personality;
    private Integer sweetness;
    private Integer floral;
    private Integer fresh;
    private Integer woody;
    private String intensity;
    private boolean generatedFromPurchaseHistory;
}
