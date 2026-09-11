package com.example.spring_boot_project_api.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TelegramLoginRequest(
        @NotNull(message = "Telegram user ID is required")
        Long id,

        @NotBlank(message = "Telegram first name is required")
        String firstName,

        String lastName,
        String username,
        String photoUrl,

        @NotNull(message = "Telegram auth_date is required")
        Long authDate,

        @NotBlank(message = "Telegram hash is required")
        String hash) {
}