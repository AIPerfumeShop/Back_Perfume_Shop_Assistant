package com.example.spring_boot_project_api.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid") String email,
        @NotBlank(message = "Verification code is required")
        @Pattern(regexp = "\\d{6}", message = "Verification code must be 6 digits") String otp) {
}