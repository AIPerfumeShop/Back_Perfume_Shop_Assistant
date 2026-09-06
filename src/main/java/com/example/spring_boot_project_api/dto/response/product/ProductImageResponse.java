package com.example.spring_boot_project_api.dto.response.product;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductImageResponse {
    private Long id;
    private Long productId;
    private String imageUrl;
    private Boolean isPrimary;
    private Integer displayOrder;
}
