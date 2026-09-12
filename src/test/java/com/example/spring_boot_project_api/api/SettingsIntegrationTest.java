package com.example.spring_boot_project_api.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class SettingsIntegrationTest {

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
        adminBearer = bearer(user(Role.ADMIN, "admin@settings.test"));
        customerBearer = bearer(user(Role.CUSTOMER, "customer@settings.test"));
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
    void settings_requiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/settings").header("Authorization", customerBearer))
                .andExpect(status().isForbidden());
    }

    @Test
    void createSetting_thenGetByKey() throws Exception {
        mockMvc.perform(post("/api/settings")
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"settingKey\":\"site-name\",\"value\":\"AI Perfume Shop\","
                                + "\"description\":\"Store display name\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.settingKey").value("site-name"))
                .andExpect(jsonPath("$.value").value("AI Perfume Shop"));

        mockMvc.perform(get("/api/settings/key/site-name")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("AI Perfume Shop"));

        mockMvc.perform(get("/api/settings").header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void createSetting_duplicateKey_returnsConflict() throws Exception {
        String payload = "{\"settingKey\":\"dupe\",\"value\":\"v1\"}";
        mockMvc.perform(post("/api/settings")
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/settings")
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void updateSetting_updatesValue() throws Exception {
        mockMvc.perform(post("/api/settings")
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"settingKey\":\"support-email\",\"value\":\"old@shop.com\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/settings/1")
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"settingKey\":\"support-email\",\"value\":\"new@shop.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("new@shop.com"));
    }
}