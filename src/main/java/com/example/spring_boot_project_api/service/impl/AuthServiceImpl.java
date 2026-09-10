package com.example.spring_boot_project_api.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.config.JwtTokenProvider;
import com.example.spring_boot_project_api.dto.request.auth.LoginRequest;
import com.example.spring_boot_project_api.dto.request.auth.RegisterRequest;
import com.example.spring_boot_project_api.dto.response.auth.AuthResponse;
import com.example.spring_boot_project_api.dto.response.user.UserResponse;
import com.example.spring_boot_project_api.enums.Role;
import com.example.spring_boot_project_api.exception.ConflictException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.exception.UnauthorizedException;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String BCRYPT_PREFIX = "$2";

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository, JwtTokenProvider tokenProvider,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (Boolean.TRUE.equals(userRepository.existsByEmail(email))) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        user.setRole(Role.CUSTOMER);
        user.setIsActive(true);
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        verifyPassword(request.password(), user);

        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new UnauthorizedException("Account has been deleted");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new UnauthorizedException("Account is disabled");
        }

        return buildAuthResponse(user);
    }

    @Override
    public UserResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return UserMapper.toUserResponse(user);
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Authentication required");
        }
        if (!tokenProvider.validate(token)) {
            throw new UnauthorizedException("Invalid token");
        }
        tokenProvider.blacklist(token);
    }

    private void verifyPassword(String rawPassword, User user) {
        String stored = user.getPassword();
        boolean matches;
        if (stored.startsWith(BCRYPT_PREFIX)) {
            matches = passwordEncoder.matches(rawPassword, stored);
        } else {
            matches = MessageDigest.isEqual(
                    rawPassword.getBytes(StandardCharsets.UTF_8),
                    stored.getBytes(StandardCharsets.UTF_8));
            if (matches) {
                user.setPassword(passwordEncoder.encode(rawPassword));
                userRepository.save(user);
            }
        }
        if (!matches) {
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = tokenProvider.generateToken(user);
        return AuthResponse.of(token, tokenProvider.getExpirationMs(), UserMapper.toUserResponse(user));
    }
}