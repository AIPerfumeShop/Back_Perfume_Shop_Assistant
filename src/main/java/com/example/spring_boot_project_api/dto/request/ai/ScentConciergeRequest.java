package com.example.spring_boot_project_api.dto.request.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScentConciergeRequest {
    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description must be under 1000 characters")
    private String description;

    private Integer limit;
}