package com.example.spring_boot_project_api.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name is required") @Size(max = 100, message = "Name must be at most 100 characters") String name,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") @Size(max = 100, message = "Email must be at most 100 characters") String email,
        @NotBlank(message = "Password is required") @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters") String password,
        @Size(max = 30, message = "Phone must be at most 30 characters") String phone) {
}