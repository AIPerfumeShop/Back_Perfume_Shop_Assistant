package com.example.spring_boot_project_api.dto.response.auth;

import com.example.spring_boot_project_api.enums.Role;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        Long userId,
        String name,
        String email,
        Role role) {

    public static AuthResponse of(String token, long expiresIn, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresIn, user.id(), user.name(), user.email(), user.role());
    }
}