package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.auth.TelegramLoginRequest;
import com.example.spring_boot_project_api.dto.response.auth.AuthResponse;

public interface TelegramAuthService {

    AuthResponse telegramLogin(TelegramLoginRequest request);

    boolean isEnabled();

    String clientId();
}