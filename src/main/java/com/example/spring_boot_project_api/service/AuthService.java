package com.example.spring_boot_project_api.service;

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

public interface AuthService {

    void register(RegisterRequest request);

    AuthResponse verifyEmail(VerifyOtpRequest request);

    void resendOtp(ResendOtpRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse me(Long userId);

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    void logout(String token);

    void forgotPassword(ForgotPasswordRequest request);

    void verifyOtp(VerifyOtpRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    void requestChangeEmail(Long userId, ChangeEmailRequest request);

    void verifyChangeEmail(Long userId, ChangeEmailVerifyRequest request);
}