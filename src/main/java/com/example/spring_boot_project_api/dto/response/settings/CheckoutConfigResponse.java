package com.example.spring_boot_project_api.dto.response.settings;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CheckoutConfigResponse {
    private final boolean khqrEnabled;
    private final long paymentExpirySeconds;
}