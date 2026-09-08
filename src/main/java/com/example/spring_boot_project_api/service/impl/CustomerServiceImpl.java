package com.example.spring_boot_project_api.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.customer.CustomerFilterRequest;
import com.example.spring_boot_project_api.dto.request.customer.CustomerUpdateRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.customer.CustomerDetailResponse;
import com.example.spring_boot_project_api.dto.response.customer.CustomerSummaryResponse;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.CustomerMapper;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.OrderRepository.UserOrderStat;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.specification.CustomerSpecification;
import com.example.spring_boot_project_api.service.CustomerService;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CustomerMapper customerMapper;

    public CustomerServiceImpl(UserRepository userRepository,
            OrderRepository orderRepository,
            CustomerMapper customerMapper) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.customerMapper = customerMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CustomerSummaryResponse> getAllCustomers(CustomerFilterRequest filter) {
        if (filter == null) {
            filter = new CustomerFilterRequest();
        }

        Page<User> users = userRepository.findAll(
                CustomerSpecification.fromFilter(filter),
                filter.toPageRequest());

        List<Long> userIds = users.getContent().stream()
                .map(User::getId)
                .toList();

        Map<Long, UserOrderStat> statsByUserId = userIds.isEmpty()
                ? Map.of()
                : orderRepository.countOrdersByUserIds(userIds).stream()
                        .collect(Collectors.toMap(
                                UserOrderStat::getUserId,
                                stat -> stat));

        List<CustomerSummaryResponse> content = users.map(user -> {
            UserOrderStat stat = statsByUserId.get(user.getId());
            Long orderCount = stat != null ? stat.getOrderCount() : 0L;
            java.math.BigDecimal totalSpent = stat != null ? stat.getTotalSpent() : java.math.BigDecimal.ZERO;
            return customerMapper.toSummaryResponse(user, orderCount, totalSpent);
        }).getContent();

        return new PagedResponse<>(
                content,
                users.getTotalElements(),
                users.getTotalPages(),
                users.getNumber(),
                users.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDetailResponse getCustomerById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer not found with ID : " + id));

        List<Order> allOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(id);
        long totalOrders = allOrders.size();
        java.math.BigDecimal totalSpent = allOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        List<Order> recentOrders = allOrders.stream()
                .limit(5)
                .toList();

        return customerMapper.toDetailResponse(user, totalOrders, totalSpent, recentOrders);
    }

    @Override
    public CustomerSummaryResponse updateCustomer(Long id, CustomerUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer not found with ID : " + id));

        if (request == null) {
            throw new BadRequestException("Update request cannot be null");
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
                throw new BadRequestException("Email already exists : " + request.getEmail());
            }
        }

        customerMapper.updateEntity(request, user);
        User updatedUser = userRepository.save(user);

        List<Order> allOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(id);
        long orderCount = allOrders.size();
        java.math.BigDecimal totalSpent = allOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        return customerMapper.toSummaryResponse(updatedUser, orderCount, totalSpent);
    }

    @Override
    public void activateCustomer(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer not found with ID : " + id));

        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadRequestException("Customer is already active");
        }

        user.setIsActive(true);
        userRepository.save(user);
    }

    @Override
    public void deactivateCustomer(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer not found with ID : " + id));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadRequestException("Customer is already deactivated");
        }

        user.setIsActive(false);
        userRepository.save(user);
    }
}
