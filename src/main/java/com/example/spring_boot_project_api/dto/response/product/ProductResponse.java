package com.example.spring_boot_project_api.dto.response.product;

import java.util.List;

import com.example.spring_boot_project_api.enums.Gender;

import lombok.Data;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private String brand;
    private String description;
    private Long categoryId;
    private String categoryName;
    private List<ProductVariantResponse> variant;
    private Gender gender;
    private String fragranceFamily;
    private String fragNotes;
    private String intensity;
    private List<String> images;
    private Double averageRate;
    private Integer reviewCount;
    private Boolean inStock;
    private Boolean active;
}
