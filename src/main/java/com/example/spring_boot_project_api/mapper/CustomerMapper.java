package com.example.spring_boot_project_api.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.customer.CustomerUpdateRequest;
import com.example.spring_boot_project_api.dto.response.customer.CustomerDetailResponse;
import com.example.spring_boot_project_api.dto.response.customer.CustomerSummaryResponse;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.User;

@Component
public class CustomerMapper {

    public CustomerSummaryResponse toSummaryResponse(User user, Long orderCount, BigDecimal totalSpent) {
        if (user == null) {
            return null;
        }
        CustomerSummaryResponse response = new CustomerSummaryResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setIsActive(user.getIsActive());
        response.setOrderCount(orderCount != null ? orderCount : 0L);
        response.setTotalSpent(totalSpent != null ? totalSpent : BigDecimal.ZERO);
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    public CustomerDetailResponse toDetailResponse(User user, Long totalOrders,
            BigDecimal totalSpent, List<Order> recentOrders) {
        if (user == null) {
            return null;
        }
        CustomerDetailResponse response = new CustomerDetailResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setIsActive(user.getIsActive());
        response.setTotalOrders(totalOrders != null ? totalOrders : 0L);
        response.setTotalSpent(totalSpent != null ? totalSpent : BigDecimal.ZERO);
        response.setRecentOrders(recentOrders != null
                ? recentOrders.stream().map(this::toRecentOrder).collect(Collectors.toList())
                : List.of());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    public void updateEntity(CustomerUpdateRequest request, User user) {
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
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
    }

    private CustomerDetailResponse.RecentOrder toRecentOrder(Order order) {
        CustomerDetailResponse.RecentOrder recentOrder = new CustomerDetailResponse.RecentOrder();
        recentOrder.setId(order.getId());
        recentOrder.setTotalAmount(order.getTotalAmount());
        recentOrder.setStatus(order.getStatus());
        recentOrder.setCreatedAt(order.getCreatedAt());
        return recentOrder;
    }
}
