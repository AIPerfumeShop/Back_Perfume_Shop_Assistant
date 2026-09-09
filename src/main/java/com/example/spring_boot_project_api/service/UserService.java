package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.user.CreateUserRequest;
import com.example.spring_boot_project_api.dto.request.user.UpdateUserRequest;
import com.example.spring_boot_project_api.dto.response.user.UserResponse;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(Long id, UpdateUserRequest request);

    void softDeleteUser(Long id);
}