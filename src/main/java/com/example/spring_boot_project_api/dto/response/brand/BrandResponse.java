package com.example.spring_boot_project_api.dto.response.brand;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrandResponse {
    private Long id;
    private String name;
    private String description;
    private String logoUrl;
    private Boolean isActive;
    private Long productsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}