package com.example.spring_boot_project_api.api;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.enums.OtpPurpose;
import com.example.spring_boot_project_api.service.EmailService;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AuthOtpHardeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailService emailService;

    private static final String EMAIL = "otp-hardening@it.test";

    @BeforeEach
    void setUp() {
        reset(emailService);
    }

    private String registerBody() {
        return """
                {
                  "name": "Otp Hardening",
                  "email": "%s",
                  "password": "password123",
                  "phone": "0123456789"
                }
                """.formatted(EMAIL);
    }

    private String register() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody()))
                .andExpect(status().isCreated());
        return captureLatestOtp();
    }

    private String captureLatestOtp() throws Exception {
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, timeout(2000).atLeastOnce()).sendOtp(eq(EMAIL), otpCaptor.capture(), eq(OtpPurpose.REGISTRATION));
        List<String> values = otpCaptor.getAllValues();
        return values.get(values.size() - 1);
    }

    private String wrongOtp(String realOtp) {
        return realOtp.equals("00000000") ? "11111111" : "00000000";
    }

    private void verifyEmail(String otp) throws Exception {
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"otp\":\"%s\"}".formatted(EMAIL, otp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void wrongOtpAttempts_triggerLockout() throws Exception {
        String otp = register();
        String wrong = wrongOtp(otp);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"%s\",\"otp\":\"%s\"}".formatted(EMAIL, wrong)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Invalid or expired")));
        }

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"otp\":\"%s\"}".formatted(EMAIL, otp)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void correctOtp_verifiesBeforeAnyLimit() throws Exception {
        String otp = register();
        verifyEmail(otp);
    }

    @Test
    void resendOtp_resetsAttemptCounter() throws Exception {
        String firstOtp = register();
        String wrong = wrongOtp(firstOtp);

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"%s\",\"otp\":\"%s\"}".formatted(EMAIL, wrong)))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/auth/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(EMAIL)))
                .andExpect(status().isOk());

        String freshOtp = captureLatestOtp();
        verifyEmail(freshOtp);
    }
}