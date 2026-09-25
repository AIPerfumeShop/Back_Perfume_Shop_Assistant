package com.example.spring_boot_project_api.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.model.Settings;
import com.example.spring_boot_project_api.repository.SettingsRepository;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CheckoutConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SettingsRepository settingsRepository;

    @Value("${payment.bakong.payment-expiry-minutes:15}")
    private long paymentExpiryMinutes;

    @BeforeEach
    void clean() {
        settingsRepository.deleteAll();
    }

    @Test
    void config_defaultsToEnabledAndConfiguredExpiry() throws Exception {
        mockMvc.perform(get("/api/checkout/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.khqrEnabled").value(true))
                .andExpect(jsonPath("$.paymentExpirySeconds")
                        .value(Math.max(1, paymentExpiryMinutes) * 60));
    }

    @Test
    void config_reflectsDisabledKhqrToggle() throws Exception {
        Settings settings = new Settings();
        settings.setSettingKey("payment_khqr");
        settings.setValue("false");
        settingsRepository.save(settings);

        mockMvc.perform(get("/api/checkout/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.khqrEnabled").value(false));
    }
}