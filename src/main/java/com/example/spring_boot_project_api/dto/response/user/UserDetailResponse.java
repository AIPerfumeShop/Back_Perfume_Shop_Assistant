package com.example.spring_boot_project_api.dto.response.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.enums.Role;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDetailResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String userImageUrl;
    private Role role;
    private Boolean isActive;
    private Long totalOrders;
    private BigDecimal totalSpent;
    private List<RecentOrder> recentOrders;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    public static class RecentOrder {
        private Long id;
        private BigDecimal totalAmount;
        private OrderStatus status;
        private LocalDateTime createdAt;
    }
}