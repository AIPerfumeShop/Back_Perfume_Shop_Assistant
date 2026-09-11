package com.example.spring_boot_project_api.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

public record TelegramLoginRequest(
        @NotBlank(message = "Telegram ID token is required")
        String idToken) {
}