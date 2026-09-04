package com.example.spring_boot_project_api.dto.response.brand;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrandStatisticsResponse {
    private Long brandId;
    private String brandName;
    private Long totalProducts;
    private Long activeProducts;
    private Long inactiveProducts;
}