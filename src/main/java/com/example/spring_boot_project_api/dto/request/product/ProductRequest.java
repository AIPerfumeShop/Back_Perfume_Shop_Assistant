package com.example.spring_boot_project_api.dto.request.product;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {
    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must be under 200 characters")
    private String name;

    private String description;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @NotBlank(message = "Brand is required")
    @Size(max = 100, message = "Brand must be under 100 characters")
    private String brand;

    private String gender;

    private String fragranceFamily;

    private String intensity;

    private List<String> fragNotes;

    private List<String> images;

    @Valid
    private List<ProductVariantRequest> variants;

    private Boolean active;
}