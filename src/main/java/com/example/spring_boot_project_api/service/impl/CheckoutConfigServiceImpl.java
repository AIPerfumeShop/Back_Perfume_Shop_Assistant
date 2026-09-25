package com.example.spring_boot_project_api.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.spring_boot_project_api.dto.response.settings.CheckoutConfigResponse;
import com.example.spring_boot_project_api.repository.SettingsRepository;
import com.example.spring_boot_project_api.service.CheckoutConfigService;

@Service
public class CheckoutConfigServiceImpl implements CheckoutConfigService {

    private static final String KHQR_ENABLED_KEY = "payment_khqr";

    private final SettingsRepository settingsRepository;
    private final long paymentExpiryMinutes;

    public CheckoutConfigServiceImpl(SettingsRepository settingsRepository,
                                     @Value("${payment.bakong.payment-expiry-minutes:15}")
                                     long paymentExpiryMinutes) {
        this.settingsRepository = settingsRepository;
        this.paymentExpiryMinutes = Math.max(1, paymentExpiryMinutes);
    }

    @Override
    public CheckoutConfigResponse getConfig() {
        boolean khqrEnabled = settingsRepository.findBySettingKey(KHQR_ENABLED_KEY)
                .map(settings -> {
                    String value = settings.getValue();
                    return value == null || value.isBlank()
                            || !"false".equalsIgnoreCase(value.trim());
                })
                .orElse(true);
        return new CheckoutConfigResponse(khqrEnabled, paymentExpiryMinutes * 60L);
    }
}