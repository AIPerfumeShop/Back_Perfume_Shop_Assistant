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
    private Long category_id;
    private String category_name;
    private List<ProductVariantResponse> variant;
    private Gender gender;
    private String fragrance_family;
    private String frag_notes;
    private String intensity;
    private List<String> images;
    private Double averageRate;
    private Integer reviewCount;
    private Boolean inStock;
    private Boolean active;
}
