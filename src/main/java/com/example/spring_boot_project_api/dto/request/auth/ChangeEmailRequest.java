package com.example.spring_boot_project_api.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeEmailRequest(
        @NotBlank(message = "New email is required")
        @Email(message = "Email must be valid")
        @Size(max = 150, message = "Email must be at most 150 characters") String newEmail) {
}