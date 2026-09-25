package com.example.spring_boot_project_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.spring_boot_project_api.dto.response.settings.CheckoutConfigResponse;
import com.example.spring_boot_project_api.model.Settings;
import com.example.spring_boot_project_api.repository.SettingsRepository;
import com.example.spring_boot_project_api.service.impl.CheckoutConfigServiceImpl;

@ExtendWith(MockitoExtension.class)
class CheckoutConfigServiceTest {

    @Mock
    private SettingsRepository settingsRepository;

    private CheckoutConfigServiceImpl service;

    private CheckoutConfigServiceImpl service(long expiryMinutes) {
        return new CheckoutConfigServiceImpl(settingsRepository, expiryMinutes);
    }

    private Settings setting(String value) {
        Settings settings = new Settings();
        settings.setSettingKey("payment_khqr");
        settings.setValue(value);
        return settings;
    }

    @Test
    void config_defaultsEnabledWhenSettingAbsent() {
        service = service(15);
        when(settingsRepository.findBySettingKey("payment_khqr"))
                .thenReturn(Optional.empty());

        CheckoutConfigResponse config = service.getConfig();

        assertTrue(config.isKhqrEnabled());
        assertEquals(900L, config.getPaymentExpirySeconds());
    }

    @Test
    void config_disabledWhenSettingIsFalse() {
        service = service(15);
        when(settingsRepository.findBySettingKey("payment_khqr"))
                .thenReturn(Optional.of(setting("false")));

        CheckoutConfigResponse config = service.getConfig();

        assertFalse(config.isKhqrEnabled());
        assertEquals(900L, config.getPaymentExpirySeconds());
    }

    @Test
    void config_enabledWhenSettingIsTrue() {
        service = service(15);
        when(settingsRepository.findBySettingKey("payment_khqr"))
                .thenReturn(Optional.of(setting("true")));

        CheckoutConfigResponse config = service.getConfig();

        assertTrue(config.isKhqrEnabled());
    }

    @Test
    void config_defaultsEnabledWhenSettingBlank() {
        service = service(15);
        when(settingsRepository.findBySettingKey("payment_khqr"))
                .thenReturn(Optional.of(setting("  ")));

        CheckoutConfigResponse config = service.getConfig();

        assertTrue(config.isKhqrEnabled());
    }

    @Test
    void config_expiryConvertsConfiguredMinutesToSeconds() {
        service = service(1);
        when(settingsRepository.findBySettingKey("payment_khqr"))
                .thenReturn(Optional.empty());

        CheckoutConfigResponse config = service.getConfig();

        assertEquals(60L, config.getPaymentExpirySeconds());
    }

    @Test
    void config_clampsUnderMinuteConfigToSixtySeconds() {
        service = service(0);
        when(settingsRepository.findBySettingKey("payment_khqr"))
                .thenReturn(Optional.empty());

        CheckoutConfigResponse config = service.getConfig();

        assertEquals(60L, config.getPaymentExpirySeconds());
    }
}