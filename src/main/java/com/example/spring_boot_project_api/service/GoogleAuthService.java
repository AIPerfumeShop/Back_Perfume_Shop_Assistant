package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.auth.GoogleLoginRequest;
import com.example.spring_boot_project_api.dto.response.auth.AuthResponse;

public interface GoogleAuthService {

    AuthResponse googleLogin(GoogleLoginRequest request);

    boolean isEnabled();

    String clientId();
}