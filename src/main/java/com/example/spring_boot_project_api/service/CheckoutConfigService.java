package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.response.settings.CheckoutConfigResponse;

public interface CheckoutConfigService {
    CheckoutConfigResponse getConfig();
}