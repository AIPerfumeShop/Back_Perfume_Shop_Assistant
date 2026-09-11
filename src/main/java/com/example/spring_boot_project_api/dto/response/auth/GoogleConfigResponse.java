package com.example.spring_boot_project_api.dto.response.auth;

public record GoogleConfigResponse(
        boolean enabled,
        String clientId) {

    public static GoogleConfigResponse of(boolean enabled, String clientId) {
        return new GoogleConfigResponse(enabled, clientId);
    }
}