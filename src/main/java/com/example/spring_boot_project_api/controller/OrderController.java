package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.order.CancelOrderRequest;
import com.example.spring_boot_project_api.dto.request.order.CheckoutRequest;
import com.example.spring_boot_project_api.dto.request.order.CreateOrderRequest;
import com.example.spring_boot_project_api.dto.request.order.UpdateOrderStatusRequest;
import com.example.spring_boot_project_api.dto.response.order.CheckoutResponse;
import com.example.spring_boot_project_api.dto.response.order.OrderResponse;
import com.example.spring_boot_project_api.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    //Create order
    @Operation(summary = "Create a new order")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order created successfully")
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    //Checkout (create order + payment)
    @Operation(summary = "Checkout (create order plus payment)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Checkout completed successfully")
    })
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @Valid @RequestBody CheckoutRequest request) {
        CheckoutResponse response = orderService.checkout(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    //Get order by id
    @Operation(summary = "Get an order by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @RequestParam Long userId,
            @PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id, userId);
        return ResponseEntity.ok(response);
    }

    //Get all orders of a user
    @Operation(summary = "Get all orders of a user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getUserOrders(
            @PathVariable Long userId) {
        List<OrderResponse> response = orderService.getUserOrders(userId);
        return ResponseEntity.ok(response);
    }

    //Get all orders (admin)
    @Operation(summary = "Get all orders (admin)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> response = orderService.getAllOrders();
        return ResponseEntity.ok(response);
    }

    //Update order status
    @Operation(summary = "Update order status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order status updated successfully")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse response =
                orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }

    //Cancel order
    @Operation(summary = "Cancel an order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order cancelled successfully")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<OrderResponse> cancelOrder(
            @RequestParam Long userId,
            @PathVariable Long id,
            @Valid @RequestBody CancelOrderRequest request) {
        OrderResponse response =
                orderService.cancelOrder(id, userId, request.getReason());
        return ResponseEntity.ok(response);
    }
}