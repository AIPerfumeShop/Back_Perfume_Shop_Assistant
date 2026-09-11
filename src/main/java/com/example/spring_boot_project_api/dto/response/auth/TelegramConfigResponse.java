package com.example.spring_boot_project_api.dto.response.auth;

public record TelegramConfigResponse(
        boolean enabled,
        String clientId) {

    public static TelegramConfigResponse of(boolean enabled, String clientId) {
        return new TelegramConfigResponse(enabled, clientId);
    }
}