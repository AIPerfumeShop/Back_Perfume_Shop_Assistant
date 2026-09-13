package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.example.spring_boot_project_api.dto.request.order.CheckoutRequest;
import com.example.spring_boot_project_api.dto.request.order.CreateOrderRequest;
import com.example.spring_boot_project_api.dto.response.order.CheckoutResponse;
import com.example.spring_boot_project_api.dto.response.order.OrderResponse;
import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.exception.ForbiddenException;
import com.example.spring_boot_project_api.exception.InvalidOrderException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.OrderService;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

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

    private OrderResponse orderResponse() {
        OrderResponse response = new OrderResponse();
        response.setId(1L);
        response.setUserId(10L);
        response.setUserName("Chan Dara");
        response.setTotalAmount(new BigDecimal("59.50"));
        response.setStatus(OrderStatus.PENDING);
        response.setShippingAddress("Phnom Penh");
        response.setPhone("012345678");
        response.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        return response;
    }

    // ---------- createOrder ----------

    @Test
    void createOrder_returns201() throws Exception {
        authenticateUser(10L);
        when(orderService.createOrder(eq(10L), any(CreateOrderRequest.class))).thenReturn(orderResponse());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shippingAddress\": \"Phnom Penh\", \"phone\": \"012345678\", \"items\": [{\"variantId\": 5, \"quantity\": 1}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userName").value("Chan Dara"));

        verify(orderService).createOrder(eq(10L), any(CreateOrderRequest.class));
    }

    @Test
    void createOrder_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shippingAddress\": \"Phnom Penh\", \"phone\": \"012345678\", \"items\": [{\"variantId\": 5, \"quantity\": 1}]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createOrder_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_userNotFound_returns404() throws Exception {
        authenticateUser(10L);
        when(orderService.createOrder(eq(10L), any(CreateOrderRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shippingAddress\": \"Phnom Penh\", \"phone\": \"012345678\", \"items\": [{\"variantId\": 5, \"quantity\": 1}]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrder_insufficientStock_returns400() throws Exception {
        authenticateUser(10L);
        when(orderService.createOrder(eq(10L), any(CreateOrderRequest.class)))
                .thenThrow(new InvalidOrderException("Insufficient stock for product : Idole"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shippingAddress\": \"Phnom Penh\", \"phone\": \"012345678\", \"items\": [{\"variantId\": 5, \"quantity\": 100}]}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- checkout ----------

    @Test
    void checkout_returns201() throws Exception {
        authenticateUser(10L);
        CheckoutResponse checkout = new CheckoutResponse();
        checkout.setOrderId(1L);
        checkout.setPaymentId(1L);
        checkout.setTransactionId("TXN-123");
        checkout.setTotalAmount(new BigDecimal("59.50"));
        checkout.setOrderStatus("PENDING");
        when(orderService.checkout(eq(10L), any(CheckoutRequest.class))).thenReturn(checkout);

        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shippingAddress\": \"Phnom Penh\", \"phone\": \"012345678\", \"items\": [{\"variantId\": 5, \"quantity\": 1}], \"paymentMethod\": \"ABA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.transactionId").value("TXN-123"))
                .andExpect(jsonPath("$.totalAmount").value(59.50));
    }

    @Test
    void checkout_missingPaymentMethod_returns400() throws Exception {
        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shippingAddress\": \"Phnom Penh\", \"phone\": \"012345678\", \"items\": []}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_userNotFound_returns404() throws Exception {
        authenticateUser(10L);
        when(orderService.checkout(eq(10L), any(CheckoutRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shippingAddress\": \"Phnom Penh\", \"phone\": \"012345678\", \"items\": [{\"variantId\": 5, \"quantity\": 1}], \"paymentMethod\": \"ABA\"}"))
                .andExpect(status().isNotFound());
    }

    // ---------- getOrderById ----------

    @Test
    void getOrderById_returns200() throws Exception {
        authenticateUser(10L);
        when(orderService.getOrderById(1L, 10L)).thenReturn(orderResponse());

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getOrderById_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOrderById_notFound_returns404() throws Exception {
        authenticateUser(10L);
        when(orderService.getOrderById(404L, 10L))
                .thenThrow(new ResourceNotFoundException("Order not found"));

        mockMvc.perform(get("/api/orders/404"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrderById_forbidden_returns403() throws Exception {
        authenticateUser(10L);
        when(orderService.getOrderById(1L, 10L))
                .thenThrow(new ForbiddenException("You do not have access to this order"));

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isForbidden());
    }

    // ---------- getUserOrders ----------

    @Test
    void getUserOrders_returnsList() throws Exception {
        authenticateUser(10L);
        when(orderService.getUserOrders(10L)).thenReturn(List.of(orderResponse()));

        mockMvc.perform(get("/api/orders/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userName").value("Chan Dara"));
    }

    @Test
    void getUserOrders_emptyList() throws Exception {
        authenticateUser(10L);
        when(orderService.getUserOrders(10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/orders/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---------- getAllOrders ----------

    @Test
    void getAllOrders_returnsList() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of(orderResponse()));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    // ---------- updateOrderStatus ----------

    @Test
    void updateOrderStatus_acceptsBody() throws Exception {
        when(orderService.updateOrderStatus(1L, OrderStatus.SHIPPED)).thenReturn(orderResponse());

        mockMvc.perform(put("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"SHIPPED\"}"))
                .andExpect(status().isOk());

        verify(orderService).updateOrderStatus(1L, OrderStatus.SHIPPED);
    }

    @Test
    void updateOrderStatus_missingStatus_returns400() throws Exception {
        mockMvc.perform(put("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateOrderStatus_invalidStatus_returns400() throws Exception {
        mockMvc.perform(put("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"INVALID\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateOrderStatus_orderNotFound_returns404() throws Exception {
        when(orderService.updateOrderStatus(404L, OrderStatus.SHIPPED))
                .thenThrow(new ResourceNotFoundException("Order not found"));

        mockMvc.perform(put("/api/orders/404/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"SHIPPED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateOrderStatus_cancelledOrder_returns400() throws Exception {
        when(orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED))
                .thenThrow(new InvalidOrderException("Cancelled order cannot change status"));

        mockMvc.perform(put("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- cancelOrder ----------

    @Test
    void cancelOrder_acceptsReason() throws Exception {
        authenticateUser(10L);
        when(orderService.cancelOrder(1L, 10L, "Changed mind")).thenReturn(orderResponse());

        mockMvc.perform(delete("/api/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Changed mind\"}"))
                .andExpect(status().isOk());

        verify(orderService).cancelOrder(1L, 10L, "Changed mind");
    }

    @Test
    void cancelOrder_noReasonStillOk() throws Exception {
        authenticateUser(10L);
        when(orderService.cancelOrder(1L, 10L, null)).thenReturn(orderResponse());

        mockMvc.perform(delete("/api/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(orderService).cancelOrder(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(10L), any());
    }

    @Test
    void cancelOrder_alreadyCancelled_returns400() throws Exception {
        authenticateUser(10L);
        when(orderService.cancelOrder(1L, 10L, "again"))
                .thenThrow(new InvalidOrderException("Order is already cancelled"));

        mockMvc.perform(delete("/api/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"again\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelOrder_forbidden_returns403() throws Exception {
        authenticateUser(10L);
        when(orderService.cancelOrder(1L, 10L, "no"))
                .thenThrow(new ForbiddenException("You do not have access to this order"));

        mockMvc.perform(delete("/api/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"no\"}"))
                .andExpect(status().isForbidden());
    }
}