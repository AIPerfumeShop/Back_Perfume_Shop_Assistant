package com.example.spring_boot_project_api.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeEmailVerifyRequest(
        @NotBlank(message = "Verification code is required")
        @Pattern(regexp = "\\d{8}", message = "Verification code must be 8 digits") String otp) {
}