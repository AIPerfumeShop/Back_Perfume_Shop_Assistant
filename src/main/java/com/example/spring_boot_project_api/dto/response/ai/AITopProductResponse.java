package com.example.spring_boot_project_api.dto.response.ai;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AITopProductResponse {
    private Long productId;
    private String productName;
    private String brand;
    private Long count;
}