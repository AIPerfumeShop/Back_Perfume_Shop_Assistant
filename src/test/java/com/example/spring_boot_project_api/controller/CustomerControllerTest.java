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

import com.example.spring_boot_project_api.dto.request.customer.CustomerFilterRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.customer.CustomerDetailResponse;
import com.example.spring_boot_project_api.dto.response.customer.CustomerSummaryResponse;
import com.example.spring_boot_project_api.enums.Role;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.service.CustomerService;

@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    private CustomerSummaryResponse summary() {
        CustomerSummaryResponse response = new CustomerSummaryResponse();
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

    private CustomerDetailResponse detail() {
        CustomerDetailResponse response = new CustomerDetailResponse();
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

    // ---------- getAllCustomers ----------

    @Test
    void getAllCustomers_returnsPaged() throws Exception {
        when(customerService.getAllCustomers(any(CustomerFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(summary()), 1, 1, 0, 20));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Chan Dara"))
                .andExpect(jsonPath("$.data[0].orderCount").value(3))
                .andExpect(jsonPath("$.data[0].totalSpent").value(180.00));
    }

    @Test
    void getAllCustomers_emptyResult() throws Exception {
        when(customerService.getAllCustomers(any(CustomerFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(), 0, 0, 0, 20));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getAllCustomers_withSearchFilter() throws Exception {
        when(customerService.getAllCustomers(any(CustomerFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(summary()), 1, 1, 0, 20));

        mockMvc.perform(get("/api/customers").param("search", "Chan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Chan Dara"));
    }

    // ---------- getCustomerById ----------

    @Test
    void getCustomerById_returnsDetail() throws Exception {
        when(customerService.getCustomerById(1L)).thenReturn(detail());

        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Chan Dara"))
                .andExpect(jsonPath("$.totalOrders").value(3))
                .andExpect(jsonPath("$.totalSpent").value(180.00));
    }

    @Test
    void getCustomerById_notFound_returns404() throws Exception {
        when(customerService.getCustomerById(404L))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/customers/404"))
                .andExpect(status().isNotFound());
    }

    // ---------- updateCustomer ----------

    @Test
    void updateCustomer_acceptsBody() throws Exception {
        when(customerService.updateCustomer(org.mockito.ArgumentMatchers.eq(1L), any()))
                .thenReturn(summary());

        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"New Name\", \"email\": \"new@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chan Dara"));

        verify(customerService).updateCustomer(org.mockito.ArgumentMatchers.eq(1L), any());
    }

    @Test
    void updateCustomer_notFound_returns404() throws Exception {
        when(customerService.updateCustomer(org.mockito.ArgumentMatchers.eq(404L), any()))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(put("/api/customers/404")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"New Name\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCustomer_duplicateEmail_returns400() throws Exception {
        when(customerService.updateCustomer(org.mockito.ArgumentMatchers.eq(1L), any()))
                .thenThrow(new BadRequestException("Email already exists"));

        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"taken@example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- activateCustomer ----------

    @Test
    void activateCustomer_returns204() throws Exception {
        mockMvc.perform(patch("/api/customers/1/activate"))
                .andExpect(status().isNoContent());

        verify(customerService).activateCustomer(1L);
    }

    @Test
    void activateCustomer_alreadyActive_returns400() throws Exception {
        org.mockito.Mockito.doThrow(new BadRequestException("Customer is already active"))
                .when(customerService).activateCustomer(1L);

        mockMvc.perform(patch("/api/customers/1/activate"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void activateCustomer_notFound_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("User not found"))
                .when(customerService).activateCustomer(404L);

        mockMvc.perform(patch("/api/customers/404/activate"))
                .andExpect(status().isNotFound());
    }

    // ---------- deactivateCustomer ----------

    @Test
    void deactivateCustomer_returns204() throws Exception {
        mockMvc.perform(patch("/api/customers/1/deactivate"))
                .andExpect(status().isNoContent());

        verify(customerService).deactivateCustomer(1L);
    }

    @Test
    void deactivateCustomer_alreadyInactive_returns400() throws Exception {
        org.mockito.Mockito.doThrow(new BadRequestException("Customer is already deactivated"))
                .when(customerService).deactivateCustomer(1L);

        mockMvc.perform(patch("/api/customers/1/deactivate"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deactivateCustomer_notFound_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("User not found"))
                .when(customerService).deactivateCustomer(404L);

        mockMvc.perform(patch("/api/customers/404/deactivate"))
                .andExpect(status().isNotFound());
    }
}