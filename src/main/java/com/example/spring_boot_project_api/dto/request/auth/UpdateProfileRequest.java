package com.example.spring_boot_project_api.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be under 100 characters") String name,
        @Size(max = 30, message = "Phone must be under 30 characters") String phone) {
}