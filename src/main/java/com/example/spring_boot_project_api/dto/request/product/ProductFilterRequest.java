package com.example.spring_boot_project_api.dto.request.product;

import java.math.BigDecimal;

import com.example.spring_boot_project_api.enums.Gender;

import lombok.Data;

@Data
public class ProductFilterRequest {
    private String search;
    private Long category_id;
    private String brand;
    private Gender gender;
    private String fragrance_family;
    private BigDecimal maxPrice;
    private BigDecimal minPrice;
    private Integer minRate;
    private Boolean Instock; 
}
