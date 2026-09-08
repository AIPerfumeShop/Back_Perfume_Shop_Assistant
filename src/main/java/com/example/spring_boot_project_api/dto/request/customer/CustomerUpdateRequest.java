package com.example.spring_boot_project_api.dto.request.customer;

import com.example.spring_boot_project_api.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerUpdateRequest {
    @Size(max = 100, message = "Full name must be under 100 characters")
    private String name;

    @Email(message = "Email must be valid")
    @Size(max = 150, message = "Email must be under 150 characters")
    private String email;

    @Size(max = 30, message = "Phone must be under 30 characters")
    private String phone;

    private Role role;
}
