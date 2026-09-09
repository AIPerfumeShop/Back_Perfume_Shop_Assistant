package com.example.spring_boot_project_api.mapper;

import com.example.spring_boot_project_api.dto.response.user.UserResponse;
import com.example.spring_boot_project_api.model.User;

public class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getIsActive(),
                user.getCreatedAt());
    }
}