package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.example.spring_boot_project_api.dto.request.order.OrderFilterRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.order.AdminOrderSummaryResponse;
import com.example.spring_boot_project_api.dto.response.order.OrderResponse;
import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.exception.InvalidOrderException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.service.OrderService;

@WebMvcTest(AdminOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

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

    // ---------- getAllOrders ----------

    @Test
    void getAllOrders_returnsPaged() throws Exception {
        AdminOrderSummaryResponse summary = new AdminOrderSummaryResponse();
        summary.setId(1L);
        summary.setUserId(10L);
        summary.setUserName("Chan Dara");
        summary.setTotalAmount(new BigDecimal("59.50"));
        summary.setStatus(OrderStatus.PENDING);
        summary.setItemCount(1);
        when(orderService.getAllOrdersFiltered(any(OrderFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(summary), 1, 1, 0, 20));

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAllOrders_emptyResult() throws Exception {
        when(orderService.getAllOrdersFiltered(any(OrderFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(), 0, 0, 0, 20));

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getAllOrders_withStatusFilter() throws Exception {
        when(orderService.getAllOrdersFiltered(any(OrderFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(), 0, 0, 0, 20));

        mockMvc.perform(get("/api/admin/orders").param("status", "SHIPPED"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllOrders_withPagination() throws Exception {
        when(orderService.getAllOrdersFiltered(any(OrderFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(), 0, 0, 1, 10));

        mockMvc.perform(get("/api/admin/orders")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.pageSize").value(10));
    }

    // ---------- getOrderById ----------

    @Test
    void getOrderById_returnsOrder() throws Exception {
        when(orderService.getOrderByIdAdmin(1L)).thenReturn(orderResponse());

        mockMvc.perform(get("/api/admin/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.userName").value("Chan Dara"));
    }

    @Test
    void getOrderById_notFound_returns404() throws Exception {
        when(orderService.getOrderByIdAdmin(404L))
                .thenThrow(new ResourceNotFoundException("Order not found with ID : 404"));

        mockMvc.perform(get("/api/admin/orders/404"))
                .andExpect(status().isNotFound());
    }

    // ---------- updateOrderStatus ----------

    @Test
    void updateOrderStatus_acceptsBody() throws Exception {
        when(orderService.updateOrderStatus(eq(1L), eq(OrderStatus.SHIPPED)))
                .thenReturn(orderResponse());

        mockMvc.perform(put("/api/admin/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"SHIPPED\"}"))
                .andExpect(status().isOk());

        verify(orderService).updateOrderStatus(1L, OrderStatus.SHIPPED);
    }

    @Test
    void updateOrderStatus_missingStatus_returns400() throws Exception {
        mockMvc.perform(put("/api/admin/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateOrderStatus_invalidStatus_returns400() throws Exception {
        mockMvc.perform(put("/api/admin/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"INVALID\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateOrderStatus_orderNotFound_returns404() throws Exception {
        when(orderService.updateOrderStatus(404L, OrderStatus.SHIPPED))
                .thenThrow(new ResourceNotFoundException("Order not found"));

        mockMvc.perform(put("/api/admin/orders/404/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"SHIPPED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateOrderStatus_cancelledOrder_returns400() throws Exception {
        when(orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED))
                .thenThrow(new InvalidOrderException("Cancelled order cannot change status"));

        mockMvc.perform(put("/api/admin/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONFIRMED\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- cancelOrder ----------

    @Test
    void cancelOrder_succeedsWithoutBody() throws Exception {
        when(orderService.cancelOrderAdmin(eq(1L), eq(null))).thenReturn(orderResponse());

        mockMvc.perform(patch("/api/admin/orders/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(orderService).cancelOrderAdmin(1L, null);
    }

    @Test
    void cancelOrder_withReason() throws Exception {
        when(orderService.cancelOrderAdmin(eq(1L), eq("Policy violation")))
                .thenReturn(orderResponse());

        mockMvc.perform(patch("/api/admin/orders/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Policy violation\"}"))
                .andExpect(status().isOk());

        verify(orderService).cancelOrderAdmin(1L, "Policy violation");
    }

    @Test
    void cancelOrder_alreadyCancelled_returns400() throws Exception {
        when(orderService.cancelOrderAdmin(eq(1L), any()))
                .thenThrow(new InvalidOrderException("Order is already cancelled"));

        mockMvc.perform(patch("/api/admin/orders/1/cancel"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelOrder_notFound_returns404() throws Exception {
        when(orderService.cancelOrderAdmin(eq(404L), any()))
                .thenThrow(new ResourceNotFoundException("Order not found"));

        mockMvc.perform(patch("/api/admin/orders/404/cancel"))
                .andExpect(status().isNotFound());
    }
}