package com.example.spring_boot_project_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
import com.example.spring_boot_project_api.service.impl.DashboardServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private Order newOrder(Long id, String customerName, BigDecimal amount, OrderStatus status) {
        User user = new User();
        user.setId(1L);
        user.setName(customerName);
        user.setEmail(customerName.toLowerCase() + "@test.com");
        Order order = new Order();
        order.setId(id);
        order.setUser(user);
        order.setTotalAmount(amount);
        order.setStatus(status);
        order.setCreatedAt(LocalDateTime.of(2026, 9, 1, 10, 0));
        return order;
    }

    private BestSellerProjection newProjection(String product, String brand, Long qty, BigDecimal revenue) {
        return new BestSellerProjection() {
            @Override
            public String getProductName() {
                return product;
            }

            @Override
            public String getBrand() {
                return brand;
            }

            @Override
            public Long getQuantitySold() {
                return qty;
            }

            @Override
            public BigDecimal getTotalRevenue() {
                return revenue;
            }
        };
    }

    @Test
    void getSummary_aggregatesCountsRevenueOrdersAndBestSellers() {
        Order order = newOrder(1L, "Alice", new BigDecimal("99.50"), OrderStatus.DELIVERED);

        when(userRepository.countByRole(Role.CUSTOMER)).thenReturn(10L);
        when(productRepository.count()).thenReturn(25L);
        when(orderRepository.count()).thenReturn(3L);
        when(orderRepository.sumTotalAmount()).thenReturn(new BigDecimal("299.50"));
        when(orderRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(List.of(order));
        when(orderRepository.countByStatus(OrderStatus.PENDING)).thenReturn(1L);
        when(orderRepository.countByStatus(OrderStatus.DELIVERED)).thenReturn(2L);
        when(orderItemRepository.findTop5BestSellers())
                .thenReturn(List.of(newProjection("Rose 100", "BrandX", 5L, new BigDecimal("250.00"))));

        DashboardSummaryResponse summary = dashboardService.getSummary();

        assertEquals(10L, summary.getTotalCustomers());
        assertEquals(25L, summary.getTotalProducts());
        assertEquals(3L, summary.getTotalOrders());
        assertEquals(new BigDecimal("299.50"), summary.getTotalRevenue());
        assertEquals(1L, summary.getOrderStatusCounts().get(OrderStatus.PENDING));
        assertEquals(2L, summary.getOrderStatusCounts().get(OrderStatus.DELIVERED));

        assertEquals(1, summary.getRecentOrders().size());
        RecentOrderResponse recent = summary.getRecentOrders().get(0);
        assertEquals("Alice", recent.getCustomerName());
        assertEquals(new BigDecimal("99.50"), recent.getTotalAmount());
        assertEquals(OrderStatus.DELIVERED, recent.getStatus());

        assertEquals(1, summary.getBestSellers().size());
        BestSellerResponse best = summary.getBestSellers().get(0);
        assertEquals("Rose 100", best.getProductName());
        assertEquals("BrandX", best.getBrand());
        assertEquals(5L, best.getQuantitySold());
        assertEquals(new BigDecimal("250.00"), best.getTotalRevenue());
    }

    @Test
    void getSummary_nullUserSafe() {
        Order order = new Order();
        order.setId(2L);
        order.setTotalAmount(new BigDecimal("10.00"));
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        when(userRepository.countByRole(Role.CUSTOMER)).thenReturn(0L);
        when(productRepository.count()).thenReturn(0L);
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.sumTotalAmount()).thenReturn(BigDecimal.ZERO);
        when(orderRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(List.of(order));
        when(orderItemRepository.findTop5BestSellers()).thenReturn(List.of());

        DashboardSummaryResponse summary = dashboardService.getSummary();

        assertEquals(1, summary.getRecentOrders().size());
        RecentOrderResponse recent = summary.getRecentOrders().get(0);
        assertNull(recent.getCustomerName());
        assertEquals(new BigDecimal("10.00"), recent.getTotalAmount());
    }
}