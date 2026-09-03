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
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    //Checkout (create order + payment)
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @Valid @RequestBody CheckoutRequest request) {
        CheckoutResponse response = orderService.checkout(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    //Get order by id
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @RequestParam Long userId,
            @PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id, userId);
        return ResponseEntity.ok(response);
    }

    //Get all orders of a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getUserOrders(
            @PathVariable Long userId) {
        List<OrderResponse> response = orderService.getUserOrders(userId);
        return ResponseEntity.ok(response);
    }

    //Get all orders (admin)
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> response = orderService.getAllOrders();
        return ResponseEntity.ok(response);
    }

    //Update order status
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse response =
                orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }

    //Cancel order
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