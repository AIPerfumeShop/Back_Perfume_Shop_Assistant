package com.example.spring_boot_project_api.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class UserManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private EmailService emailService;

    private User admin;
    private User customerOne;
    private User customerTwo;
    private User adminUser;

    @BeforeEach
    void setUp() {
        admin = user(Role.ADMIN, "admin@manage.test", "Root Admin");
        customerOne = user(Role.CUSTOMER, "alice@manage.test", "Alice");
        customerTwo = user(Role.CUSTOMER, "bob@manage.test", "Bob");
        adminUser = user(Role.ADMIN, "staff@manage.test", "Staff");
    }

    private User user(Role role, String email, String name) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("password123");
        user.setRole(role);
        return userRepository.save(user);
    }

    private String bearer(User user) {
        return "Bearer " + tokenProvider.generateToken(user);
    }

    @Test
    void userManagement_requiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", bearer(customerOne)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(customerOne))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacker\",\"email\":\"hack@test.com\","
                                + "\"password\":\"password123\",\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_searchesAndFilters() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4));

        mockMvc.perform(get("/api/users?search=alice").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].email").value("alice@manage.test"));

        mockMvc.perform(get("/api/users?role=ADMIN").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void createUser_createsAndThenLoginWorks() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Person\",\"email\":\"new@manage.test\","
                                + "\"password\":\"password123\",\"phone\":\"0123456789\",\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Person"))
                .andExpect(jsonPath("$.email").value("new@manage.test"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.isActive").value(true));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@manage.test\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void createUser_duplicateEmail_returnsConflict() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dup\",\"email\":\"alice@manage.test\","
                                + "\"password\":\"password123\",\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateUser_updatesFields() throws Exception {
        User target = customerOne;
        mockMvc.perform(put("/api/users/" + target.getId())
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice Updated\",\"phone\":\"0999999999\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Updated"));
    }

    @Test
    void activateDeactivate_cyclesStatus() throws Exception {
        User target = customerOne;

        mockMvc.perform(patch("/api/users/" + target.getId() + "/deactivate")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users?isActive=false").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(patch("/api/users/" + target.getId() + "/activate")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/" + target.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void softDeleteUser_deactivatesTarget() throws Exception {
        User target = customerTwo;

        mockMvc.perform(delete("/api/users/" + target.getId())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        User stored = userRepository.findById(target.getId()).orElseThrow();
        assert Boolean.TRUE.equals(stored.getIsDeleted());
        assert !Boolean.TRUE.equals(stored.getIsActive());

        mockMvc.perform(get("/api/users/" + target.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        mockMvc.perform(get("/api/users?isActive=false").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void softDelete_yourOwnAccount_returnsBadRequest() throws Exception {
        mockMvc.perform(delete("/api/users/" + admin.getId())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest());
    }
}