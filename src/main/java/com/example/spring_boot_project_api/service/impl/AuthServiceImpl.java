package com.example.spring_boot_project_api.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.PasswordResetToken;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.PasswordResetTokenRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.AuthService;
import com.example.spring_boot_project_api.service.EmailService;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String BCRYPT_PREFIX = "$2";
    private static final int OTP_TTL_MINUTES = 10;
    private static final int OTP_LENGTH = 6;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           JwtTokenProvider tokenProvider,
                           PasswordEncoder passwordEncoder,
                           EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
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
        user.setIsActive(false);
        userRepository.save(user);

        issueOtp(user, OtpPurpose.REGISTRATION, null);
    }

    @Override
    @Transactional
    public AuthResponse verifyEmail(VerifyOtpRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification code"));

        PasswordResetToken token = validateOtp(user.getId(), request.otp());
        consumeToken(token, user);

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            user.setIsActive(true);
            userRepository.save(user);
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        String email = request.email().trim().toLowerCase();
        userRepository.findByEmail(email).ifPresent(user -> {
            if (Boolean.TRUE.equals(user.getIsActive())) {
                return;
            }
            passwordResetTokenRepository.deleteByUserId(user.getId());
            issueOtp(user, OtpPurpose.REGISTRATION, null);
        });
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
            throw new UnauthorizedException("Account is not verified. Please check your email for the activation code.");
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
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            user.setPhone(request.phone().trim());
        }
        userRepository.save(user);
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

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase();
        userRepository.findByEmail(email).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUserId(user.getId());
            issueOtp(user, OtpPurpose.PASSWORD_RESET, null);
        });
    }

    @Override
    public void verifyOtp(VerifyOtpRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification code"));
        validateOtp(user.getId(), request.otp());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification code"));

        PasswordResetToken token = validateOtp(user.getId(), request.otp());
        if (Boolean.TRUE.equals(user.getIsDeleted()) || !Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadRequestException("Account is disabled or deleted");
        }
        consumeToken(token, user);

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!passwordMatches(request.currentPassword(), user)) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        passwordResetTokenRepository.deleteByUserId(user.getId());
    }

    @Override
    @Transactional
    public void requestChangeEmail(Long userId, ChangeEmailRequest request) {
        String newEmail = request.newEmail().trim().toLowerCase();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (newEmail.equals(user.getEmail())) {
            throw new BadRequestException("New email must be different from the current email");
        }
        if (Boolean.TRUE.equals(userRepository.existsByEmail(newEmail))) {
            throw new ConflictException("This email is already in use");
        }

        passwordResetTokenRepository.deleteByUserId(user.getId());
        issueOtp(user, OtpPurpose.EMAIL_CHANGE, newEmail);
    }

    @Override
    @Transactional
    public void verifyChangeEmail(Long userId, ChangeEmailVerifyRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        PasswordResetToken token = validateOtp(user.getId(), request.otp());
        String newEmail = token.getContext();
        if (newEmail == null || newEmail.isBlank()) {
            throw new BadRequestException("Invalid or expired verification code");
        }

        if (Boolean.TRUE.equals(userRepository.existsByEmail(newEmail))) {
            throw new ConflictException("This email is already in use");
        }

        consumeToken(token, user);
        user.setEmail(newEmail);
        userRepository.save(user);
    }

    private void issueOtp(User user, OtpPurpose purpose, String context) {
        String recipient = purpose == OtpPurpose.EMAIL_CHANGE ? context : user.getEmail();
        String otp = generateOtp();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setOtpHash(sha256Hex(otp));
        resetToken.setUserId(user.getId());
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES));
        resetToken.setContext(context);
        passwordResetTokenRepository.save(resetToken);
        emailService.sendOtp(recipient, otp, purpose);
    }

    private PasswordResetToken validateOtp(Long userId, String rawOtp) {
        PasswordResetToken token = passwordResetTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification code"));
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired verification code");
        }
        if (!MessageDigest.isEqual(
                sha256Hex(rawOtp.trim()).getBytes(StandardCharsets.UTF_8),
                token.getOtpHash().getBytes(StandardCharsets.UTF_8))) {
            throw new BadRequestException("Invalid or expired verification code");
        }
        return token;
    }

    private void consumeToken(PasswordResetToken token, User user) {
        passwordResetTokenRepository.deleteByUserId(user.getId());
    }

    private boolean passwordMatches(String rawPassword, User user) {
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
        return matches;
    }

    private void verifyPassword(String rawPassword, User user) {
        if (!passwordMatches(rawPassword, user)) {
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = tokenProvider.generateToken(user);
        return AuthResponse.of(token, tokenProvider.getExpirationMs(), UserMapper.toUserResponse(user));
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        return String.format("%0" + OTP_LENGTH + "d", random.nextInt((int) Math.pow(10, OTP_LENGTH)));
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available on this JVM", e);
        }
    }
}