package com.example.spring_boot_project_api.dto.request.user;

import com.example.spring_boot_project_api.enums.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be under 100 characters")
    private String name;

    @Size(max = 30, message = "Phone must be under 30 characters")
    private String phone;

    @NotNull(message = "Role is required")
    private Role role;

    @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
    private String password;
}