package com.example.spring_boot_project_api.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import com.example.spring_boot_project_api.dto.request.user.UpdateUserRequest;
import com.example.spring_boot_project_api.dto.response.user.UserDetailResponse;
import com.example.spring_boot_project_api.dto.response.user.UserResponse;
import com.example.spring_boot_project_api.dto.response.user.UserSummaryResponse;
import com.example.spring_boot_project_api.model.Order;
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
                user.getUserImageUrl(),
                user.getRole(),
                user.getIsActive(),
                user.getCreatedAt());
    }

    public static UserSummaryResponse toSummaryResponse(User user, Long orderCount, BigDecimal totalSpent) {
        if (user == null) {
            return null;
        }
        UserSummaryResponse response = new UserSummaryResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setUserImageUrl(user.getUserImageUrl());
        response.setRole(user.getRole());
        response.setIsActive(user.getIsActive());
        response.setOrderCount(orderCount != null ? orderCount : 0L);
        response.setTotalSpent(totalSpent != null ? totalSpent : BigDecimal.ZERO);
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    public static UserDetailResponse toDetailResponse(User user, Long totalOrders,
            BigDecimal totalSpent, List<Order> recentOrders) {
        if (user == null) {
            return null;
        }
        UserDetailResponse response = new UserDetailResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setUserImageUrl(user.getUserImageUrl());
        response.setRole(user.getRole());
        response.setIsActive(user.getIsActive());
        response.setTotalOrders(totalOrders != null ? totalOrders : 0L);
        response.setTotalSpent(totalSpent != null ? totalSpent : BigDecimal.ZERO);
        response.setRecentOrders(recentOrders != null
                ? recentOrders.stream().map(UserMapper::toRecentOrder).collect(Collectors.toList())
                : List.of());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    public static void updateEntity(UpdateUserRequest request, User user) {
        if (request == null || user == null) {
            return;
        }
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getUserImageUrl() != null) {
            String imageUrl = request.getUserImageUrl().trim();
            user.setUserImageUrl(imageUrl.isEmpty() ? null : imageUrl);
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
    }

    private static UserDetailResponse.RecentOrder toRecentOrder(Order order) {
        UserDetailResponse.RecentOrder recentOrder = new UserDetailResponse.RecentOrder();
        recentOrder.setId(order.getId());
        recentOrder.setTotalAmount(order.getTotalAmount());
        recentOrder.setStatus(order.getStatus());
        recentOrder.setCreatedAt(order.getCreatedAt());
        return recentOrder;
    }
}