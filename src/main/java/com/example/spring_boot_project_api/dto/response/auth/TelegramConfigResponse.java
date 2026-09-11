package com.example.spring_boot_project_api.dto.response.auth;

public record TelegramConfigResponse(
        boolean enabled,
        String botUsername) {

    public static TelegramConfigResponse of(boolean enabled, String botUsername) {
        return new TelegramConfigResponse(enabled, botUsername);
    }
}