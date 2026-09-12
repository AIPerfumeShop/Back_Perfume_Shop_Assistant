package com.example.spring_boot_project_api.dto.request.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductVariantRequest {
    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must be under 100 characters")
    private String sku;

    @NotNull(message = "Size is required")
    @Min(value = 1, message = "Size must be positive")
    private Integer sizeMl;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    private BigDecimal price;

    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    private Boolean isActive;
}