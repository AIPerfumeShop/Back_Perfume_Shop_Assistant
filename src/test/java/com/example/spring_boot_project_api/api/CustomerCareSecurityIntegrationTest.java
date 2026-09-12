package com.example.spring_boot_project_api.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CustomerCareSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private EmailService emailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String adminToken;
    private String customerToken;
    private User customerA;
    private User customerB;

    @BeforeEach
    void setUp() {
        adminToken = token(user(Role.ADMIN, "admin@cs-sec.test"));
        customerA = user(Role.CUSTOMER, "customer-a@cs-sec.test");
        customerB = user(Role.CUSTOMER, "customer-b@cs-sec.test");
        customerToken = token(customerA);
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

    private long handoffTicket(User user, String reason) throws Exception {
        String body = mockMvc.perform(post("/api/cs/handoff")
                        .header("Authorization", token(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"" + reason + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("id").asLong();
    }

    @Test
    void csCustomerEndpoints_derivedFromJwtNoUserIdParam() throws Exception {
        mockMvc.perform(get("/api/cs/tickets")
                        .header("Authorization", customerToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/cs/handoff")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"why hello\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void csEndpoints_stillRequireAuth() throws Exception {
        mockMvc.perform(get("/api/cs/tickets"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/cs/queue"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/cs/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void csAgentReads_adminOnly() throws Exception {
        mockMvc.perform(get("/api/cs/queue")
                        .header("Authorization", customerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/cs/analytics")
                        .header("Authorization", customerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/cs/queue")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void csAgentWrites_adminOnly() throws Exception {
        mockMvc.perform(post("/api/cs/tickets/999/reply")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/cs/tickets/999/status")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/cs/tickets/999/context")
                        .header("Authorization", customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void csOwnership_ignoresUserIdParamBlocksIdor() throws Exception {
        long ticketId = handoffTicket(customerA, "idors attempt");

        mockMvc.perform(get("/api/cs/tickets/" + ticketId)
                        .header("Authorization", token(customerB))
                        .param("userId", String.valueOf(customerA.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/cs/tickets/" + ticketId + "/messages")
                        .header("Authorization", token(customerB))
                        .param("userId", String.valueOf(customerA.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"can you see me?\"}"))
                .andExpect(status().isForbidden());
    }
}