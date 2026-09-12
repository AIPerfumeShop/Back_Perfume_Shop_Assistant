package com.example.spring_boot_project_api.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.config.JwtTokenProvider;
import com.example.spring_boot_project_api.enums.Role;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.EmailService;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AIAnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private EmailService emailService;

    private String adminBearer;
    private String customerBearer;

    @BeforeEach
    void setUp() {
        adminBearer = bearer(user(Role.ADMIN, "admin@aianalytics.test"));
        customerBearer = bearer(user(Role.CUSTOMER, "customer@aianalytics.test"));
    }

    private User user(Role role, String email) {
        User user = new User();
        user.setName(role.name());
        user.setEmail(email);
        user.setPassword("password123");
        user.setRole(role);
        return userRepository.save(user);
    }

    private String bearer(User user) {
        return "Bearer " + tokenProvider.generateToken(user);
    }

    @Test
    void aiAnalytics_requiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/ai/analytics/usage").header("Authorization", customerBearer))
                .andExpect(status().isForbidden());
    }

    @Test
    void allAIEndpoints_respondOnEmptyData() throws Exception {
        mockMvc.perform(get("/api/ai/analytics/recommendations").header("Authorization", adminBearer))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/ai/analytics/clicks").header("Authorization", adminBearer))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/ai/analytics/dashboard").header("Authorization", adminBearer))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/ai/analytics/conversations").header("Authorization", adminBearer))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/ai/analytics/messages").header("Authorization", adminBearer))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/ai/analytics/usage").header("Authorization", adminBearer))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/ai/analytics/trends").header("Authorization", adminBearer))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/ai/analytics/popular-questions").header("Authorization", adminBearer))
                .andExpect(status().isOk());
    }
}