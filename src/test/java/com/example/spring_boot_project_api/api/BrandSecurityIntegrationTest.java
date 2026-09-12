package com.example.spring_boot_project_api.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
class BrandSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private EmailService emailService;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        adminToken = token(user(Role.ADMIN, "admin@brand-sec.test"));
        customerToken = token(user(Role.CUSTOMER, "customer@brand-sec.test"));
    }

    private User user(Role role, String email) {
        User user = new User();
        user.setName(role.name());
        user.setEmail(email);
        user.setPassword("password123");
        user.setRole(role);
        return userRepository.save(user);
    }

    private String token(User user) {
        return "Bearer " + tokenProvider.generateToken(user);
    }

    private String brandPayload() {
        return "{\"name\":\"Security Test Brand\",\"description\":\"test\"}";
    }

    @Test
    void brandWrites_requireAuth() throws Exception {
        mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brandPayload()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void brandWrites_rejectNonAdmin() throws Exception {
        mockMvc.perform(post("/api/brands")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brandPayload()))
                .andExpect(status().isForbidden());
    }

    @Test
    void brandWrites_allowedForAdmin() throws Exception {
        mockMvc.perform(post("/api/brands")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brandPayload()))
                .andExpect(status().isCreated());
    }

    @Test
    void brandReads_stayPublic() throws Exception {
        mockMvc.perform(get("/api/brands"))
                .andExpect(status().isOk());
    }

    @Test
    void csEndpoints_requireAuth() throws Exception {
        mockMvc.perform(get("/api/cs/queue"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/cs/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
    }
}