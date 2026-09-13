package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.config.JwtTokenProvider;
import com.example.spring_boot_project_api.dto.request.user.UserFilterRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.user.UserDetailResponse;
import com.example.spring_boot_project_api.dto.response.user.UserSummaryResponse;
import com.example.spring_boot_project_api.enums.Role;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.UserService;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private UserSummaryResponse summary() {
        UserSummaryResponse response = new UserSummaryResponse();
        response.setId(1L);
        response.setName("Chan Dara");
        response.setEmail("dara@example.com");
        response.setPhone("012345678");
        response.setRole(Role.CUSTOMER);
        response.setIsActive(true);
        response.setOrderCount(3L);
        response.setTotalSpent(new BigDecimal("180.00"));
        response.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        return response;
    }

    private UserDetailResponse detail() {
        UserDetailResponse response = new UserDetailResponse();
        response.setId(1L);
        response.setName("Chan Dara");
        response.setEmail("dara@example.com");
        response.setPhone("012345678");
        response.setRole(Role.CUSTOMER);
        response.setIsActive(true);
        response.setTotalOrders(3L);
        response.setTotalSpent(new BigDecimal("180.00"));
        response.setRecentOrders(List.of());
        return response;
    }

    // ---------- getAllUsers ----------

    @Test
    void getAllUsers_returnsPaged() throws Exception {
        when(userService.getAllUsers(any(UserFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(summary()), 1, 1, 0, 20));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Chan Dara"))
                .andExpect(jsonPath("$.data[0].orderCount").value(3))
                .andExpect(jsonPath("$.data[0].totalSpent").value(180.00));
    }

    @Test
    void getAllUsers_emptyResult() throws Exception {
        when(userService.getAllUsers(any(UserFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(), 0, 0, 0, 20));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getAllUsers_withSearchFilter() throws Exception {
        when(userService.getAllUsers(any(UserFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(summary()), 1, 1, 0, 20));

        mockMvc.perform(get("/api/users").param("search", "Chan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Chan Dara"));
    }

    // ---------- getUserById ----------

    @Test
    void getUserById_returnsDetail() throws Exception {
        when(userService.getUserById(1L)).thenReturn(detail());

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Chan Dara"))
                .andExpect(jsonPath("$.totalOrders").value(3))
                .andExpect(jsonPath("$.totalSpent").value(180.00));
    }

    @Test
    void getUserById_notFound_returns404() throws Exception {
        when(userService.getUserById(404L))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/404"))
                .andExpect(status().isNotFound());
    }

    // ---------- updateUser ----------

    @Test
    void updateUser_acceptsBody() throws Exception {
        when(userService.updateUser(org.mockito.ArgumentMatchers.eq(1L), any()))
                .thenReturn(summary());

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"New Name\", \"email\": \"new@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chan Dara"));

        verify(userService).updateUser(org.mockito.ArgumentMatchers.eq(1L), any());
    }

    @Test
    void updateUser_notFound_returns404() throws Exception {
        when(userService.updateUser(org.mockito.ArgumentMatchers.eq(404L), any()))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(put("/api/users/404")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"New Name\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_duplicateEmail_returns400() throws Exception {
        when(userService.updateUser(org.mockito.ArgumentMatchers.eq(1L), any()))
                .thenThrow(new BadRequestException("Email already exists"));

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"taken@example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- activateUser ----------

    @Test
    void activateUser_returns204() throws Exception {
        mockMvc.perform(patch("/api/users/1/activate"))
                .andExpect(status().isNoContent());

        verify(userService).activateUser(1L);
    }

    @Test
    void activateUser_alreadyActive_returns400() throws Exception {
        org.mockito.Mockito.doThrow(new BadRequestException("User is already active"))
                .when(userService).activateUser(1L);

        mockMvc.perform(patch("/api/users/1/activate"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void activateUser_notFound_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("User not found"))
                .when(userService).activateUser(404L);

        mockMvc.perform(patch("/api/users/404/activate"))
                .andExpect(status().isNotFound());
    }

    // ---------- deactivateUser ----------

    @Test
    void deactivateUser_returns204() throws Exception {
        mockMvc.perform(patch("/api/users/1/deactivate"))
                .andExpect(status().isNoContent());

        verify(userService).deactivateUser(1L);
    }

    @Test
    void deactivateUser_alreadyInactive_returns400() throws Exception {
        org.mockito.Mockito.doThrow(new BadRequestException("User is already deactivated"))
                .when(userService).deactivateUser(1L);

        mockMvc.perform(patch("/api/users/1/deactivate"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deactivateUser_notFound_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("User not found"))
                .when(userService).deactivateUser(404L);

        mockMvc.perform(patch("/api/users/404/deactivate"))
                .andExpect(status().isNotFound());
    }
}