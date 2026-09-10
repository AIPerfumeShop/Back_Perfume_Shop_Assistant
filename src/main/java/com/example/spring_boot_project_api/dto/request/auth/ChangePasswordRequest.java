package com.example.spring_boot_project_api.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required") String currentPassword,
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters") String newPassword) {
}