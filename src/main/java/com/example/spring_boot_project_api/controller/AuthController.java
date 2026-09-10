package com.example.spring_boot_project_api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import com.example.spring_boot_project_api.dto.response.auth.MessageResponse;
import com.example.spring_boot_project_api.dto.response.user.UserResponse;
import com.example.spring_boot_project_api.exception.UnauthorizedException;
import com.example.spring_boot_project_api.service.AuthService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Registration successful. A verification code has been sent to your email."));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<MessageResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.ok(new MessageResponse("If an account exists for this email, a new verification code has been sent."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(new MessageResponse("If an account exists for this email, a verification code has been sent."));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<MessageResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(new MessageResponse("Verification code is valid."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Password has been reset successfully."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        Long userId = SecurityUtils.currentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
        return ResponseEntity.ok(authService.me(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = SecurityUtils.currentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("Authentication required");
        }
        authService.logout(authorization.substring(7));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = SecurityUtils.currentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
        authService.changePassword(userId, request);
        return ResponseEntity.ok(new MessageResponse("Password changed successfully."));
    }

    @PostMapping("/change-email/request")
    public ResponseEntity<MessageResponse> requestChangeEmail(@Valid @RequestBody ChangeEmailRequest request) {
        Long userId = SecurityUtils.currentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
        authService.requestChangeEmail(userId, request);
        return ResponseEntity.ok(new MessageResponse("A verification code has been sent to the new email address."));
    }

    @PostMapping("/change-email/verify")
    public ResponseEntity<MessageResponse> verifyChangeEmail(@Valid @RequestBody ChangeEmailVerifyRequest request) {
        Long userId = SecurityUtils.currentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
        authService.verifyChangeEmail(userId, request);
        return ResponseEntity.ok(new MessageResponse("Email address changed successfully."));
    }
}