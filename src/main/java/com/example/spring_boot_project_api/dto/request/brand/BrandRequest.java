package com.example.spring_boot_project_api.dto.request.brand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrandRequest {
    @NotBlank(message = "Brand name is required")
    @Size(max = 100,message = "Brand name must be under 100 characters")
    private String name;
    private String description;
    @Size(max = 500, message = "Logo url must be under 500")
    private String logoUrl;
    private Boolean isActive = true;
}