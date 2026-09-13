package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.user.CreateUserRequest;
import com.example.spring_boot_project_api.dto.request.user.UpdateUserRequest;
import com.example.spring_boot_project_api.dto.request.user.UserFilterRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.user.UserDetailResponse;
import com.example.spring_boot_project_api.dto.response.user.UserSummaryResponse;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ConflictException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.OrderRepository.UserOrderStat;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.specification.UserSpecification;
import com.example.spring_boot_project_api.service.UserService;
import com.example.spring_boot_project_api.util.SecurityUtils;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
            OrderRepository orderRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserSummaryResponse> getAllUsers(UserFilterRequest filter) {
        if (filter == null) {
            filter = new UserFilterRequest();
        }

        Page<User> users = userRepository.findAll(
                UserSpecification.fromFilter(filter),
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

        List<UserSummaryResponse> content = users.map(user -> {
            UserOrderStat stat = statsByUserId.get(user.getId());
            Long orderCount = stat != null ? stat.getOrderCount() : 0L;
            BigDecimal totalSpent = stat != null ? stat.getTotalSpent() : BigDecimal.ZERO;
            return UserMapper.toSummaryResponse(user, orderCount, totalSpent);
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
    public UserDetailResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with ID : " + id));

        List<Order> allOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(id);
        long totalOrders = allOrders.size();
        BigDecimal totalSpent = allOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Order> recentOrders = allOrders.stream()
                .limit(5)
                .toList();

        return UserMapper.toDetailResponse(user, totalOrders, totalSpent, recentOrders);
    }

    @Override
    @Transactional
    public UserSummaryResponse createUser(CreateUserRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (Boolean.TRUE.equals(userRepository.existsByEmail(email))) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone() == null || request.getPhone().isBlank() ? null : request.getPhone().trim());
        user.setUserImageUrl(request.getUserImageUrl() == null || request.getUserImageUrl().isBlank()
                ? null
                : request.getUserImageUrl().trim());
        user.setRole(request.getRole());
        user.setIsActive(true);
        user.setIsDeleted(false);
        userRepository.save(user);

        return UserMapper.toSummaryResponse(user, 0L, BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public UserSummaryResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request == null) {
            throw new BadRequestException("Update request cannot be null");
        }

        String newEmail = request.getEmail();
        if (newEmail != null && !newEmail.equalsIgnoreCase(user.getEmail())
                && Boolean.TRUE.equals(userRepository.existsByEmailAndIdNot(newEmail, id))) {
            throw new BadRequestException("Email already exists : " + newEmail);
        }

        UserMapper.updateEntity(request, user);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        User updatedUser = userRepository.save(user);

        List<Order> allOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(id);
        long orderCount = allOrders.size();
        BigDecimal totalSpent = allOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return UserMapper.toSummaryResponse(updatedUser, orderCount, totalSpent);
    }

    @Override
    @Transactional
    public void softDeleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (SecurityUtils.currentUserId().map(currentId -> currentId.equals(id)).orElse(false) && user.getRole() != null) {
            throw new BadRequestException("You cannot delete your own account");
        }
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new BadRequestException("User is already deleted");
        }

        user.setIsDeleted(true);
        user.setIsActive(false);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with ID : " + id));

        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadRequestException("User is already active");
        }

        user.setIsActive(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with ID : " + id));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadRequestException("User is already deactivated");
        }

        user.setIsActive(false);
        userRepository.save(user);
    }
}