package com.example.spring_boot_project_api.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class PaymentSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private EmailService emailService;

    private String email;

    @BeforeEach
    void setUp() {
        Mockito.reset(emailService);
        email = "paysec" + System.nanoTime() + "@it.test";
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(emailService);
    }

    private String registerAndVerify(String mail) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Pay Security",
                                  "email": "%s",
                                  "password": "password123",
                                  "phone": "0123456789"
                                }
                                """.formatted(mail)))
                .andExpect(status().isCreated());

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, timeout(4000))
                .sendOtp(eq(mail), otpCaptor.capture(), eq(OtpPurpose.REGISTRATION));

        String token = mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"otp\":\"%s\"}".formatted(mail, otpCaptor.getValue())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(token).get("token").asText();
    }

    @Test
    void paymentHistoryMe_customerAuthenticated_isAllowed() throws Exception {
        String jwt = registerAndVerify(email);

        mockMvc.perform(get("/api/payments/history/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
    }

    @Test
    void paymentHistoryForAnyUser_customer_isForbidden() throws Exception {
        String jwt = registerAndVerify(email);

        mockMvc.perform(get("/api/payments/history/user/1")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void paymentHistoryForAnyUser_anonymous_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/payments/history/user/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void paymentVerify_anonymous_isUnauthorized() throws Exception {
        mockMvc.perform(post("/api/payments/999999/verify"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void paymentVerify_customerAuthenticated_reachesService() throws Exception {
        String jwt = registerAndVerify(email);

        mockMvc.perform(post("/api/payments/999999/verify")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void paymentHistoryOrder_customerAuthenticated_isAllowed() throws Exception {
        String jwt = registerAndVerify(email);

        mockMvc.perform(get("/api/payments/history/order/999999")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
    }
}