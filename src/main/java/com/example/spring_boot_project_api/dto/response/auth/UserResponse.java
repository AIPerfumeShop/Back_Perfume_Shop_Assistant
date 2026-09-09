package com.example.spring_boot_project_api.dto.response.auth;

import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.Role;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        Boolean isActive,
        LocalDateTime createdAt) {
}