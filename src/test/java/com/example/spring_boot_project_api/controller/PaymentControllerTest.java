package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.config.JwtTokenProvider;
import com.example.spring_boot_project_api.dto.request.payment.PaymentRequest;
import com.example.spring_boot_project_api.dto.response.payment.PaymentResponse;
import com.example.spring_boot_project_api.enums.PaymentMethod;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.PaymentService;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(Long id) {
        User principal = new User();
        principal.setId(id);
        principal.setName("Chan Dara");
        principal.setEmail("dara@example.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private PaymentResponse paymentResponse() {
        PaymentResponse response = new PaymentResponse();
        response.setId(1L);
        response.setOrderId(10L);
        response.setOrderUserId(10L);
        response.setPaymentMethod(PaymentMethod.ABA);
        response.setAmount(new BigDecimal("59.50"));
        response.setStatus(PaymentStatus.PENDING);
        response.setTransactionId("TXN-123");
        response.setErrorMessage(null);
        response.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        return response;
    }

    private PaymentResponse successfulPaymentResponse() {
        PaymentResponse response = paymentResponse();
        response.setStatus(PaymentStatus.SUCCESSFUL);
        response.setPaidAt(LocalDateTime.of(2026, 1, 1, 12, 5));
        return response;
    }

    // ---------- createPayment ----------

    @Test
    void createPayment_returns201() throws Exception {
        when(paymentService.createPayment(any(PaymentRequest.class))).thenReturn(paymentResponse());

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": 10, \"paymentMethod\": \"ABA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.amount").value(59.5))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(paymentService).createPayment(any(PaymentRequest.class));
    }

    @Test
    void createPayment_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_missingOrderId_returns400() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\": \"ABA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_missingPaymentMethod_returns400() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": 10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_invalidMethod_returns400() throws Exception {
        when(paymentService.createPayment(any(PaymentRequest.class)))
                .thenThrow(new BadRequestException("Invalid payment method"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": 10, \"paymentMethod\": \"INVALID\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_orderNotFound_returns404() throws Exception {
        when(paymentService.createPayment(any(PaymentRequest.class)))
                .thenThrow(new ResourceNotFoundException("Order not found"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": 999, \"paymentMethod\": \"ABA\"}"))
                .andExpect(status().isNotFound());
    }

    // ---------- getPayment ----------

    @Test
    void getPayment_returns200() throws Exception {
        when(paymentService.getPayment(1L)).thenReturn(paymentResponse());

        mockMvc.perform(get("/api/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.paymentMethod").value("ABA"))
                .andExpect(jsonPath("$.transactionId").value("TXN-123"));
    }

    @Test
    void getPayment_notFound_returns404() throws Exception {
        when(paymentService.getPayment(404L))
                .thenThrow(new ResourceNotFoundException("Payment not found"));

        mockMvc.perform(get("/api/payments/404"))
                .andExpect(status().isNotFound());
    }

    // ---------- getPaymentByTransactionId ----------

    @Test
    void getPaymentByTransactionId_returns200() throws Exception {
        when(paymentService.getPaymentByTransactionId("TXN-123")).thenReturn(paymentResponse());

        mockMvc.perform(get("/api/payments/transaction/TXN-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TXN-123"));
    }

    @Test
    void getPaymentByTransactionId_notFound_returns404() throws Exception {
        when(paymentService.getPaymentByTransactionId("NOPE"))
                .thenThrow(new ResourceNotFoundException("Payment not found"));

        mockMvc.perform(get("/api/payments/transaction/NOPE"))
                .andExpect(status().isNotFound());
    }

    // ---------- getPaymentStatus ----------

    @Test
    void getPaymentStatus_returns200() throws Exception {
        when(paymentService.getPaymentStatus(1L)).thenReturn(successfulPaymentResponse());

        mockMvc.perform(get("/api/payments/1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESSFUL"));
    }

    @Test
    void getPaymentStatus_notFound_returns404() throws Exception {
        when(paymentService.getPaymentStatus(404L))
                .thenThrow(new ResourceNotFoundException("Payment not found"));

        mockMvc.perform(get("/api/payments/404/status"))
                .andExpect(status().isNotFound());
    }

    // ---------- validatePayment ----------

    @Test
    void validatePayment_returns200() throws Exception {
        when(paymentService.validatePayment(1L)).thenReturn(successfulPaymentResponse());

        mockMvc.perform(post("/api/payments/1/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("SUCCESSFUL"));

        verify(paymentService).validatePayment(1L);
    }

    @Test
    void validatePayment_alreadyValidated_returns400() throws Exception {
        when(paymentService.validatePayment(1L))
                .thenThrow(new BadRequestException("Payment already successful"));

        mockMvc.perform(post("/api/payments/1/validate"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validatePayment_notFound_returns404() throws Exception {
        when(paymentService.validatePayment(404L))
                .thenThrow(new ResourceNotFoundException("Payment not found"));

        mockMvc.perform(post("/api/payments/404/validate"))
                .andExpect(status().isNotFound());
    }

    // ---------- processPayment ----------

    @Test
    void processPayment_success_returnsTrue() throws Exception {
        when(paymentService.processPayment(1L)).thenReturn(true);

        mockMvc.perform(post("/api/payments/1/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void processPayment_failure_returnsFalse() throws Exception {
        when(paymentService.processPayment(1L)).thenReturn(false);

        mockMvc.perform(post("/api/payments/1/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

    @Test
    void processPayment_alreadyProcessed_returns400() throws Exception {
        when(paymentService.processPayment(1L))
                .thenThrow(new BadRequestException("Payment already processed"));

        mockMvc.perform(post("/api/payments/1/process"))
                .andExpect(status().isBadRequest());
    }

    // ---------- getPaymentHistoryByOrder ----------

    @Test
    void getPaymentHistoryByOrder_returnsList() throws Exception {
        authenticateUser(10L);
        when(paymentService.getPaymentHistoryByOrder(10L)).thenReturn(List.of(paymentResponse()));

        mockMvc.perform(get("/api/payments/history/order/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(10));
    }

    @Test
    void getPaymentHistoryByOrder_emptyList() throws Exception {
        authenticateUser(10L);
        when(paymentService.getPaymentHistoryByOrder(10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/payments/history/order/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---------- getPaymentHistoryByUser ----------

    @Test
    void getPaymentHistoryByUser_returnsList() throws Exception {
        when(paymentService.getPaymentHistoryByUser(10L)).thenReturn(List.of(paymentResponse()));

        mockMvc.perform(get("/api/payments/history/user/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(10));

        verify(paymentService).getPaymentHistoryByUser(eq(10L));
    }

    @Test
    void getPaymentHistoryByUser_emptyList() throws Exception {
        when(paymentService.getPaymentHistoryByUser(10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/payments/history/user/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}