package com.example.spring_boot_project_api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.spring_boot_project_api.config.JwtTokenProvider;
import com.example.spring_boot_project_api.dto.request.auth.ChangeEmailRequest;
import com.example.spring_boot_project_api.dto.request.auth.ChangeEmailVerifyRequest;
import com.example.spring_boot_project_api.dto.request.auth.ChangePasswordRequest;
import com.example.spring_boot_project_api.dto.request.auth.ForgotPasswordRequest;
import com.example.spring_boot_project_api.dto.request.auth.LoginRequest;
import com.example.spring_boot_project_api.dto.request.auth.RegisterRequest;
import com.example.spring_boot_project_api.dto.request.auth.ResendOtpRequest;
import com.example.spring_boot_project_api.dto.request.auth.ResetPasswordRequest;
import com.example.spring_boot_project_api.dto.request.auth.UpdateProfileRequest;
import com.example.spring_boot_project_api.dto.request.auth.VerifyOtpRequest;
import com.example.spring_boot_project_api.dto.response.auth.AuthResponse;
import com.example.spring_boot_project_api.dto.response.user.UserResponse;
import com.example.spring_boot_project_api.enums.OtpPurpose;
import com.example.spring_boot_project_api.enums.Role;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ConflictException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.exception.UnauthorizedException;
import com.example.spring_boot_project_api.model.PasswordResetToken;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.PasswordResetTokenRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.EmailService;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret-";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    private JwtTokenProvider tokenProvider;
    private PasswordEncoder passwordEncoder;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(SECRET, 3_600_000L);
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthServiceImpl(userRepository, passwordResetTokenRepository, tokenProvider, passwordEncoder, emailService);
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

    private PasswordResetToken otpToken(Long userId, String rawOtp, LocalDateTime expiresAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(1L);
        token.setOtpHash(sha256Hex(rawOtp));
        token.setUserId(userId);
        token.setExpiresAt(expiresAt);
        return token;
    }

    private void stubTokenSave() {
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubUserSave() {
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void register_createsInactiveCustomerAndSendsOtp() {
        RegisterRequest request = new RegisterRequest("  Test User ", "TEST@Example.com", "password123", null);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        stubUserSave();
        stubTokenSave();
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);

        authService.register(request);

        verify(userRepository).save(userCaptor.capture());
        User persisted = userCaptor.getValue();
        assertTrue(passwordEncoder.matches("password123", persisted.getPassword()));
        assertEquals("test@example.com", persisted.getEmail());
        assertEquals(Role.CUSTOMER, persisted.getRole());
        assertTrue(!Boolean.TRUE.equals(persisted.getIsActive()));

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtp(eq("test@example.com"), otpCaptor.capture(), eq(OtpPurpose.REGISTRATION));
        assertTrue(otpCaptor.getValue().matches("\\d{8}"));
        assertEquals(sha256Hex(otpCaptor.getValue()), tokenCaptor.getValue().getOtpHash());
        assertTrue(tokenCaptor.getValue().getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        RegisterRequest request = new RegisterRequest("Test", "dup@example.com", "password123", null);

        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));
    }

    @Test
    void verifyEmail_validOtp_activatesAccountAndReturnsToken() {
        User user = user(10L, "new@example.com", "password123", false);
        PasswordResetToken token = otpToken(10L, "123456", LocalDateTime.now().plusMinutes(10));
        stubUserSave();
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserId(10L)).thenReturn(Optional.of(token));

        AuthResponse response = authService.verifyEmail(new VerifyOtpRequest("new@example.com", "123456"));

        assertTrue(Boolean.TRUE.equals(user.getIsActive()));
        assertEquals("new@example.com", response.email());
        assertTrue(tokenProvider.validate(response.token()));
        verify(passwordResetTokenRepository).deleteByUserId(10L);
    }

    @Test
    void verifyEmail_wrongOtp_throwsBadRequest() {
        User user = user(10L, "new@example.com", "password123", false);
        PasswordResetToken token = otpToken(10L, "000000", LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserId(10L)).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class,
                () -> authService.verifyEmail(new VerifyOtpRequest("new@example.com", "654321")));
    }

    @Test
    void verifyEmail_expiredOtp_throwsBadRequest() {
        User user = user(10L, "new@example.com", "password123", false);
        PasswordResetToken token = otpToken(10L, "123456", LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserId(10L)).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class,
                () -> authService.verifyEmail(new VerifyOtpRequest("new@example.com", "123456")));
    }

    @Test
    void resendOtp_pendingAccount_sendsFreshCode() {
        User user = user(10L, "new@example.com", "password123", false);
        stubTokenSave();
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(user));

        authService.resendOtp(new ResendOtpRequest("new@example.com"));

        verify(passwordResetTokenRepository).deleteByUserId(10L);
        verify(emailService).sendOtp(eq("new@example.com"), any(), eq(OtpPurpose.REGISTRATION));
    }

    @Test
    void resendOtp_activeAccount_doesNothing() {
        User user = user(10L, "active@example.com", "password123", true);
        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(user));

        authService.resendOtp(new ResendOtpRequest("active@example.com"));

        verifyNoInteractions(passwordResetTokenRepository, emailService);
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
    void login_unverifiedAccount_throwsUnauthorized() {
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
    void updateProfile_updatesNameAndPhone() {
        User user = user(1L, "update@example.com", "password123", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        stubUserSave();

        UserResponse response = authService.updateProfile(1L, new UpdateProfileRequest("  New Name ", "0991234567", null));

        assertEquals("New Name", response.name());
        assertEquals("0991234567", response.phone());
        assertEquals("New Name", user.getName());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_unknownUser_throwsResourceNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authService.updateProfile(99L, new UpdateProfileRequest("New Name", "0991234567", null)));
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

    @Test
    void forgotPassword_knownUser_sendsPasswordResetOtp() {
        User user = user(1L, "reset@example.com", "password123", true);
        stubTokenSave();
        when(userRepository.findByEmail("reset@example.com")).thenReturn(Optional.of(user));

        authService.forgotPassword(new ForgotPasswordRequest("reset@example.com"));

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).deleteByUserId(1L);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtp(eq("reset@example.com"), otpCaptor.capture(), eq(OtpPurpose.PASSWORD_RESET));

        PasswordResetToken saved = tokenCaptor.getValue();
        assertEquals(sha256Hex(otpCaptor.getValue()), saved.getOtpHash());
        assertEquals(1L, saved.getUserId());
        assertTrue(otpCaptor.getValue().matches("\\d{8}"));
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void forgotPassword_unknownEmail_doesNothing() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword(new ForgotPasswordRequest("nobody@example.com"));

        verifyNoInteractions(passwordResetTokenRepository, emailService);
    }

    @Test
    void verifyOtp_validCode_doesNotConsumeToken() {
        User user = user(1L, "reset@example.com", "password123", true);
        PasswordResetToken token = otpToken(1L, "123456", LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail("reset@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserId(1L)).thenReturn(Optional.of(token));

        authService.verifyOtp(new VerifyOtpRequest("reset@example.com", "123456"));

        verify(passwordResetTokenRepository, never()).deleteByUserId(any());
    }

    @Test
    void verifyOtp_wrongCode_throwsBadRequest() {
        User user = user(1L, "reset@example.com", "password123", true);
        PasswordResetToken token = otpToken(1L, "000000", LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail("reset@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserId(1L)).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class,
                () -> authService.verifyOtp(new VerifyOtpRequest("reset@example.com", "123456")));
    }

    @Test
    void resetPassword_validOtp_encodesNewPassword() {
        User user = user(1L, "reset@example.com", "oldpassword", true);
        stubUserSave();
        PasswordResetToken token = otpToken(1L, "123456", LocalDateTime.now().plusMinutes(30));
        when(userRepository.findByEmail("reset@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserId(1L)).thenReturn(Optional.of(token));

        authService.resetPassword(new ResetPasswordRequest("reset@example.com", "123456", "newpassword"));

        verify(userRepository).save(user);
        assertTrue(passwordEncoder.matches("newpassword", user.getPassword()));
        verify(passwordResetTokenRepository).deleteByUserId(1L);
    }

    @Test
    void resetPassword_unknownEmail_throwsBadRequest() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> authService.resetPassword(new ResetPasswordRequest("nobody@example.com", "123456", "newpassword")));
    }

    @Test
    void changePassword_correctCurrentPassword_encodesNewPassword() {
        User user = user(1L, "me@example.com", "oldpassword", true);
        stubUserSave();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        authService.changePassword(1L, new ChangePasswordRequest("oldpassword", "newpassword"));

        verify(userRepository).save(user);
        assertTrue(passwordEncoder.matches("newpassword", user.getPassword()));
        verify(passwordResetTokenRepository).deleteByUserId(1L);
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsUnauthorized() {
        User user = user(1L, "me@example.com", "oldpassword", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class,
                () -> authService.changePassword(1L, new ChangePasswordRequest("wrong", "newpassword")));
    }

    @Test
    void requestChangeEmail_sendsOtpToNewEmail() {
        User user = user(1L, "old@example.com", "password123", true);
        stubTokenSave();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("newaddress@example.com")).thenReturn(false);

        authService.requestChangeEmail(1L, new ChangeEmailRequest("newaddress@example.com"));

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).deleteByUserId(1L);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        assertEquals("newaddress@example.com", tokenCaptor.getValue().getContext());
        verify(emailService).sendOtp(eq("newaddress@example.com"), any(), eq(OtpPurpose.EMAIL_CHANGE));
    }

    @Test
    void requestChangeEmail_takenEmail_throwsConflict() {
        User user = user(1L, "old@example.com", "password123", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> authService.requestChangeEmail(1L, new ChangeEmailRequest("taken@example.com")));
    }

    @Test
    void requestChangeEmail_sameEmail_throwsBadRequest() {
        User user = user(1L, "old@example.com", "password123", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class,
                () -> authService.requestChangeEmail(1L, new ChangeEmailRequest("old@example.com")));
    }

    @Test
    void verifyChangeEmail_validOtp_appliesNewEmail() {
        User user = user(1L, "old@example.com", "password123", true);
        PasswordResetToken token = otpToken(1L, "123456", LocalDateTime.now().plusMinutes(10));
        token.setContext("newaddress@example.com");
        stubUserSave();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserId(1L)).thenReturn(Optional.of(token));
        when(userRepository.existsByEmail("newaddress@example.com")).thenReturn(false);

        authService.verifyChangeEmail(1L, new ChangeEmailVerifyRequest("123456"));

        verify(userRepository).save(user);
        assertEquals("newaddress@example.com", user.getEmail());
        verify(passwordResetTokenRepository).deleteByUserId(1L);
    }

    @Test
    void verifyChangeEmail_wrongOtp_throwsBadRequest() {
        User user = user(1L, "old@example.com", "password123", true);
        PasswordResetToken token = otpToken(1L, "000000", LocalDateTime.now().plusMinutes(10));
        token.setContext("newaddress@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserId(1L)).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class,
                () -> authService.verifyChangeEmail(1L, new ChangeEmailVerifyRequest("123456")));
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}