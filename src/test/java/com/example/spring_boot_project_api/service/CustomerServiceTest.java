package com.example.spring_boot_project_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.example.spring_boot_project_api.dto.request.customer.CustomerFilterRequest;
import com.example.spring_boot_project_api.dto.request.customer.CustomerUpdateRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.customer.CustomerDetailResponse;
import com.example.spring_boot_project_api.dto.response.customer.CustomerSummaryResponse;
import com.example.spring_boot_project_api.enums.Role;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.CustomerMapper;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.impl.CustomerServiceImpl;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    private CustomerServiceImpl customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(
                userRepository, orderRepository, new CustomerMapper());
    }

    private User user(Long id, String name, String email, boolean active) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setPhone("012345678");
        user.setRole(Role.CUSTOMER);
        user.setIsActive(active);
        user.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        return user;
    }

    private Order order(Long id, BigDecimal total) {
        Order order = new Order();
        order.setId(id);
        order.setTotalAmount(total);
        return order;
    }

    private static class UserOrderStatImpl implements OrderRepository.UserOrderStat {
        private final Long userId;
        private final Long orderCount;
        private final BigDecimal totalSpent;

        UserOrderStatImpl(Long userId, Long orderCount, BigDecimal totalSpent) {
            this.userId = userId;
            this.orderCount = orderCount;
            this.totalSpent = totalSpent;
        }

        @Override
        public Long getUserId() {
            return userId;
        }

        @Override
        public Long getOrderCount() {
            return orderCount;
        }

        @Override
        public BigDecimal getTotalSpent() {
            return totalSpent;
        }
    }

    // ---------- list customers ----------

    @Test
    void getAllCustomers_returnsPagedWithStats() {
        User user = user(1L, "Chan Dara", "dara@example.com", true);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(orderRepository.countOrdersByUserIds(List.of(1L)))
                .thenReturn(List.of(new UserOrderStatImpl(1L, 3L, new BigDecimal("180.00"))));

        CustomerFilterRequest filter = new CustomerFilterRequest();
        PagedResponse<CustomerSummaryResponse> response =
                customerService.getAllCustomers(filter);

        assertNotNull(response);
        assertEquals(1, response.getData().size());
        CustomerSummaryResponse summary = response.getData().get(0);
        assertEquals(1L, summary.getId());
        assertEquals("Chan Dara", summary.getName());
        assertEquals(3L, summary.getOrderCount());
        assertEquals(new BigDecimal("180.00"), summary.getTotalSpent());
        assertTrue(summary.getIsActive());
    }

    @Test
    void getAllCustomers_noOrders_statsZero() {
        User user = user(1L, "Chan Dara", "dara@example.com", true);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(orderRepository.countOrdersByUserIds(List.of(1L)))
                .thenReturn(List.of());

        CustomerFilterRequest filter = new CustomerFilterRequest();
        PagedResponse<CustomerSummaryResponse> response =
                customerService.getAllCustomers(filter);

        CustomerSummaryResponse summary = response.getData().get(0);
        assertEquals(0L, summary.getOrderCount());
        assertEquals(BigDecimal.ZERO, summary.getTotalSpent());
    }

    @Test
    void getAllCustomers_emptyPage_returnsEmpty() {
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PagedResponse<CustomerSummaryResponse> response =
                customerService.getAllCustomers(new CustomerFilterRequest());

        assertTrue(response.getData().isEmpty());
    }

    // ---------- customer detail ----------

    @Test
    void getCustomerById_returnsDetailWithRecentOrders() {
        User user = user(1L, "Chan Dara", "dara@example.com", true);
        List<Order> orders = List.of(
                order(3L, new BigDecimal("60.00")),
                order(2L, new BigDecimal("70.00")),
                order(1L, new BigDecimal("50.00")));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(orders);

        CustomerDetailResponse response = customerService.getCustomerById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(3L, response.getTotalOrders());
        assertEquals(new BigDecimal("180.00"), response.getTotalSpent());
        assertEquals(3, response.getRecentOrders().size());
    }

    @Test
    void getCustomerById_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> customerService.getCustomerById(99L));
    }

    // ---------- update customer ----------

    @Test
    void updateCustomer_updatesFields() {
        User user = user(1L, "Old Name", "old@example.com", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("new@example.com", 1L)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.setName("New Name");
        request.setEmail("new@example.com");
        request.setPhone("099000111");

        CustomerSummaryResponse response = customerService.updateCustomer(1L, request);

        assertEquals("New Name", response.getName());
        assertEquals("new@example.com", response.getEmail());
        assertEquals("099000111", response.getPhone());
        verify(userRepository).save(user);
    }

    @Test
    void updateCustomer_duplicateEmail_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "A", "a@example.com", true)));
        when(userRepository.existsByEmailAndIdNot("b@example.com", 1L)).thenReturn(true);

        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.setEmail("b@example.com");

        assertThrows(BadRequestException.class,
                () -> customerService.updateCustomer(1L, request));
    }

    @Test
    void updateCustomer_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> customerService.updateCustomer(99L, new CustomerUpdateRequest()));
    }

    // ---------- activate / deactivate ----------

    @Test
    void activateCustomer_setsActive() {
        User user = user(1L, "A", "a@example.com", false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        customerService.activateCustomer(1L);

        assertTrue(user.getIsActive());
        verify(userRepository).save(user);
    }

    @Test
    void activateCustomer_alreadyActive_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "A", "a@example.com", true)));

        assertThrows(BadRequestException.class,
                () -> customerService.activateCustomer(1L));
    }

    @Test
    void deactivateCustomer_setsInactive() {
        User user = user(1L, "A", "a@example.com", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        customerService.deactivateCustomer(1L);

        assertFalse(user.getIsActive());
    }

    @Test
    void deactivateCustomer_alreadyInactive_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "A", "a@example.com", false)));

        assertThrows(BadRequestException.class,
                () -> customerService.deactivateCustomer(1L));
    }
}