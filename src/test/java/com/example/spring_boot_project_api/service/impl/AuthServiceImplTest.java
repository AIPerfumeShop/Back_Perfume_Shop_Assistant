package com.example.spring_boot_project_api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.spring_boot_project_api.config.JwtTokenProvider;
import com.example.spring_boot_project_api.dto.request.auth.LoginRequest;
import com.example.spring_boot_project_api.dto.request.auth.RegisterRequest;
import com.example.spring_boot_project_api.dto.response.auth.AuthResponse;
import com.example.spring_boot_project_api.dto.response.user.UserResponse;
import com.example.spring_boot_project_api.enums.Role;
import com.example.spring_boot_project_api.exception.ConflictException;
import com.example.spring_boot_project_api.exception.UnauthorizedException;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret-";

    @Mock
    private UserRepository userRepository;

    private JwtTokenProvider tokenProvider;
    private PasswordEncoder passwordEncoder;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(SECRET, 3_600_000L);
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthServiceImpl(userRepository, tokenProvider, passwordEncoder);
    }

    private User user(Long id, String email, String rawPassword, boolean active) {
        User user = new User();
        user.setId(id);
        user.setName("Test User");
        user.setEmail(email);
        user.setPhone("0123456789");
        user.setRole(Role.CUSTOMER);
        user.setIsActive(active);
        user.setPassword(passwordEncoder.encode(rawPassword));
        return user;
    }

    @Test
    void register_createsCustomerWithEncodedPassword() {
        RegisterRequest request = new RegisterRequest("  Test User ", "TEST@Example.com", "password123", null);
        User saved = new User();
        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        AuthResponse response = authService.register(request);

        assertEquals(Role.CUSTOMER, response.role());
        assertEquals("test@example.com", response.email());
        assertEquals("Test User", response.name());
        assertTrue(tokenProvider.validate(response.token()));

        verify(userRepository).save(captor.capture());
        User persisted = captor.getValue();
        assertTrue(passwordEncoder.matches("password123", persisted.getPassword()));
        assertTrue(Boolean.TRUE.equals(persisted.getIsActive()));
        assertEquals("test@example.com", persisted.getEmail());
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        RegisterRequest request = new RegisterRequest("Test", "dup@example.com", "password123", null);

        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));
    }

    @Test
    void login_success_returnsValidToken() {
        User user = user(1L, "alice@example.com", "password123", true);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(new LoginRequest("alice@example.com", "password123"));

        assertEquals("alice@example.com", response.email());
        assertTrue(tokenProvider.validate(response.token()));
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        User user = user(1L, "alice@example.com", "correct", true);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("alice@example.com", "wrong")));
    }

    @Test
    void login_deletedAccount_throwsUnauthorized() {
        User user = user(1L, "gone@example.com", "password123", true);
        user.setIsDeleted(true);
        when(userRepository.findByEmail("gone@example.com")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("gone@example.com", "password123")));
    }

    @Test
    void login_inactiveAccount_throwsUnauthorized() {
        User user = user(1L, "off@example.com", "password123", false);
        when(userRepository.findByEmail("off@example.com")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("off@example.com", "password123")));
    }

    @Test
    void login_plaintextPassword_autoUpgradesToBcrypt() {
        User user = user(1L, "legacy@example.com", "ignored", true);
        user.setPassword("plainpassword");
        when(userRepository.findByEmail("legacy@example.com")).thenReturn(Optional.of(user));

        AuthResponse response =
                authService.login(new LoginRequest("legacy@example.com", "plainpassword"));

        assertTrue(passwordEncoder.matches("plainpassword", user.getPassword()));
        verify(userRepository, times(1)).save(user);
        assertTrue(tokenProvider.validate(response.token()));
    }

    @Test
    void me_returnsLoggedInUser() {
        User user = user(1L, "me@example.com", "password123", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = authService.me(1L);

        assertEquals(1L, response.id());
        assertEquals("me@example.com", response.email());
        assertEquals(Role.CUSTOMER, response.role());
        assertTrue(response.isActive());
    }

    @Test
    void logout_invalidatesToken() {
        User user = user(1L, "bye@example.com", "password123", true);
        String token = tokenProvider.generateToken(user);

        authService.logout(token);

        assertTrue(tokenProvider.isBlacklisted(token));
    }

    @Test
    void logout_invalidToken_throwsUnauthorized() {
        assertThrows(UnauthorizedException.class, () -> authService.logout("not-a-jwt"));
    }

    @Test
    void logout_blankToken_throwsUnauthorized() {
        assertThrows(UnauthorizedException.class, () -> authService.logout(" "));
    }
}