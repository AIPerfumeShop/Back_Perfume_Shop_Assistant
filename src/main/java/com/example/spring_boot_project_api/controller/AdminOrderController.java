package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.order.CancelOrderRequest;
import com.example.spring_boot_project_api.dto.request.order.OrderFilterRequest;
import com.example.spring_boot_project_api.dto.request.order.UpdateOrderStatusRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.order.AdminOrderSummaryResponse;
import com.example.spring_boot_project_api.dto.response.order.OrderResponse;
import com.example.spring_boot_project_api.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/orders")
@Validated
@RequiredArgsConstructor
public class AdminOrderController {
    private final OrderService orderService;

    //Get all orders with filters + pagination
    @Operation(summary = "Get all orders with filtering and pagination (admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<PagedResponse<AdminOrderSummaryResponse>> getAllOrders(
            @ModelAttribute OrderFilterRequest filter) {
        PagedResponse<AdminOrderSummaryResponse> response =
                orderService.getAllOrdersFiltered(filter);
        return ResponseEntity.ok(response);
    }

    //Get order details
    @Operation(summary = "Get order details (admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        OrderResponse response = orderService.getOrderByIdAdmin(id);
        return ResponseEntity.ok(response);
    }

    //Update order status
    @Operation(summary = "Update order status (admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse response =
                orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }

    //Cancel an order
    @Operation(summary = "Cancel an order (admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "400", description = "Order is already cancelled")
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelOrderRequest request) {
        OrderResponse response =
                orderService.cancelOrderAdmin(id, request == null ? null : request.getReason());
        return ResponseEntity.ok(response);
    }
}