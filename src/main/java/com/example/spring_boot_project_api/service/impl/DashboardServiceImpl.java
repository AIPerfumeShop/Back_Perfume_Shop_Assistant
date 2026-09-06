package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.response.dashboard.BestSellerResponse;
import com.example.spring_boot_project_api.dto.response.dashboard.DashboardSummaryResponse;
import com.example.spring_boot_project_api.dto.response.dashboard.RecentOrderResponse;
import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.enums.Role;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.OrderItemRepository;
import com.example.spring_boot_project_api.repository.OrderItemRepository.BestSellerProjection;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.DashboardService;

@Service
@Transactional
public class DashboardServiceImpl implements DashboardService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public DashboardServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            UserRepository userRepository,
            ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        DashboardSummaryResponse response = new DashboardSummaryResponse();

        response.setTotalCustomers(userRepository.countByRole(Role.CUSTOMER));
        response.setTotalProducts(productRepository.count());
        response.setTotalOrders(orderRepository.count());
        response.setTotalRevenue(orderRepository.sumTotalAmount());
        response.setOrderStatusCounts(countOrdersByStatus());
        response.setRecentOrders(mapRecentOrders(orderRepository.findTop10ByOrderByCreatedAtDesc()));
        response.setBestSellers(mapBestSellers(orderItemRepository.findTop5BestSellers()));

        return response;
    }

    private Map<OrderStatus, Long> countOrdersByStatus() {
        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            counts.put(status, orderRepository.countByStatus(status));
        }
        return counts;
    }

    private List<RecentOrderResponse> mapRecentOrders(List<Order> orders) {
        return orders.stream()
                .map(order -> {
                    RecentOrderResponse dto = new RecentOrderResponse();
                    dto.setId(order.getId());
                    dto.setCustomerName(customerName(order));
                    dto.setEmail(customerEmail(order));
                    dto.setTotalAmount(order.getTotalAmount());
                    dto.setStatus(order.getStatus());
                    dto.setCreatedAt(order.getCreatedAt());
                    return dto;
                })
                .toList();
    }

    private List<BestSellerResponse> mapBestSellers(List<BestSellerProjection> projections) {
        return projections.stream()
                .map(projection -> {
                    BestSellerResponse dto = new BestSellerResponse();
                    dto.setProductName(projection.getProductName());
                    dto.setBrand(projection.getBrand());
                    dto.setQuantitySold(projection.getQuantitySold() != null
                            ? projection.getQuantitySold()
                            : 0L);
                    dto.setTotalRevenue(projection.getTotalRevenue() != null
                            ? projection.getTotalRevenue()
                            : BigDecimal.ZERO);
                    return dto;
                })
                .toList();
    }

    private String customerName(Order order) {
        User user = order.getUser();
        return user != null ? user.getName() : null;
    }

    private String customerEmail(Order order) {
        User user = order.getUser();
        return user != null ? user.getEmail() : null;
    }
}