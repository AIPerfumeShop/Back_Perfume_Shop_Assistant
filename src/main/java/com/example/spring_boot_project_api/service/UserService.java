package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.user.CreateUserRequest;
import com.example.spring_boot_project_api.dto.request.user.UpdateUserRequest;
import com.example.spring_boot_project_api.dto.request.user.UserFilterRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.user.UserDetailResponse;
import com.example.spring_boot_project_api.dto.response.user.UserResponse;
import com.example.spring_boot_project_api.dto.response.user.UserSummaryResponse;

public interface UserService {

    PagedResponse<UserSummaryResponse> getAllUsers(UserFilterRequest filter);

    UserDetailResponse getUserById(Long id);

    UserSummaryResponse createUser(CreateUserRequest request);

    UserSummaryResponse updateUser(Long id, UpdateUserRequest request);

    void softDeleteUser(Long id);

    void activateUser(Long id);

    void deactivateUser(Long id);
}