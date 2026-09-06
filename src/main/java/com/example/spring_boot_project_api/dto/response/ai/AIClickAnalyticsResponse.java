package com.example.spring_boot_project_api.dto.response.ai;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AIClickAnalyticsResponse {
    private long totalClicks;
    private long uniqueUsers;
    private List<AITopProductResponse> topClickedProducts;
}