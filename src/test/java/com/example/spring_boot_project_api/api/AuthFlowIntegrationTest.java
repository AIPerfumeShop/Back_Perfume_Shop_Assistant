package com.example.spring_boot_project_api.api;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.enums.OtpPurpose;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailService emailService;

    private static final String EMAIL = "flow@it.test";

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.reset(emailService);
    }

    private String registerBody() {
        return """
                {
                  "name": "Flow Tester",
                  "email": "%s",
                  "password": "password123",
                  "phone": "0123456789"
                }
                """.formatted(EMAIL);
    }

    private String captureRegistrationOtp() throws Exception {
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, timeout(2000)).sendOtp(eq(EMAIL), otpCaptor.capture(), eq(OtpPurpose.REGISTRATION));
        return otpCaptor.getValue();
    }

    @Test
    void fullAuthLifecycle_register_verify_login_me_update_logout() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", containsString("verification code")));

        String otp = captureRegistrationOtp();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password123\"}".formatted(EMAIL)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("not verified")));

        String token = mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"otp\":\"%s\"}".formatted(EMAIL, otp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn().getResponse().getContentAsString();

        String jwt = objectMapper.readTree(token).get("token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.name").value("Flow Tester"));

        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Flow\",\"phone\":\"0990000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Flow"))
                .andExpect(jsonPath("$.phone").value("0990000000"));

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedCalls_areRejected() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Anon\",\"phone\":\"1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_duplicateEmail_isRejected() throws Exception {
        User existing = new User();
        existing.setName("Taken");
        existing.setEmail(EMAIL);
        existing.setPassword("password123");
        existing.setRole(com.example.spring_boot_project_api.enums.Role.CUSTOMER);
        userRepository.save(existing);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody()))
                .andExpect(status().isConflict());
    }
}