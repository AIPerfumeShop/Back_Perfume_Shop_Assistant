package com.example.spring_boot_project_api.dto.response.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.Role;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSummaryResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String userImageUrl;
    private Role role;
    private Boolean isActive;
    private Long orderCount;
    private BigDecimal totalSpent;
    private LocalDateTime createdAt;
}