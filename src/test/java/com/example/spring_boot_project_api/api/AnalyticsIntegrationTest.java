package com.example.spring_boot_project_api.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class AnalyticsIntegrationTest {

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
        adminBearer = bearer(user(Role.ADMIN, "admin@analytics.test"));
        customerBearer = bearer(user(Role.CUSTOMER, "customer@analytics.test"));
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
    void analytics_requiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/analytics/sales").header("Authorization", customerBearer))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSalesAnalytics_returnsZeroedMetricsOnEmptyData() throws Exception {
        mockMvc.perform(get("/api/analytics/sales").header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(0))
                .andExpect(jsonPath("$.totalOrders").value(0))
                .andExpect(jsonPath("$.averageOrderValue").value(0));
    }

    @Test
    void allAnalyticsEndpoints_respond() throws Exception {
        mockMvc.perform(get("/api/analytics/products").header("Authorization", adminBearer))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/analytics/categories").header("Authorization", adminBearer))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/analytics/brands").header("Authorization", adminBearer))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/analytics/customers").header("Authorization", adminBearer))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/analytics/dashboard").header("Authorization", adminBearer))
                .andExpect(status().isOk());
    }

    @Test
    void salesAnalytics_acceptsDateRange() throws Exception {
        mockMvc.perform(get("/api/analytics/sales?from=2026-01-01&to=2026-12-31")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk());
    }
}