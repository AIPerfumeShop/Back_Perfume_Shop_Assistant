package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.payment.PaymentRequest;
import com.example.spring_boot_project_api.dto.response.payment.PaymentResponse;
import com.example.spring_boot_project_api.exception.ForbiddenException;
import com.example.spring_boot_project_api.exception.UnauthorizedException;
import com.example.spring_boot_project_api.service.PaymentService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    //Create payment
    @Operation(summary = "Create a payment for an order")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Payment created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid payment method or order"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    //Get payment by id
    @Operation(summary = "Get a payment by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    //Get payment by transaction id
    @Operation(summary = "Get a payment by transaction ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Transaction ID is required"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<PaymentResponse> getPaymentByTransactionId(
            @PathVariable String transactionId) {
        return ResponseEntity.ok(
                paymentService.getPaymentByTransactionId(transactionId));
    }

    //Get payment status
    @Operation(summary = "Get the status of a payment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment status retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{id}/status")
    public ResponseEntity<PaymentResponse> getPaymentStatus(
            @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(id));
    }

    //Validate payment
    @Operation(summary = "Validate a payment before processing")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment is valid"),
        @ApiResponse(responseCode = "400", description = "Payment is not valid"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @PostMapping("/{id}/validate")
    public ResponseEntity<PaymentResponse> validatePayment(
            @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.validatePayment(id));
    }

    //Process payment
    @Operation(summary = "Process a payment (simulate gateway)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment processed successfully"),
        @ApiResponse(responseCode = "400", description = "Payment cannot be processed"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @PostMapping("/{id}/process")
    public ResponseEntity<Boolean> processPayment(
            @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.processPayment(id));
    }

    //Verify a KHQR payment against Bakong
    @Operation(summary = "Verify a KHQR payment status against Bakong")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment checked successfully"),
        @ApiResponse(responseCode = "400", description = "Payment has no KHQR code"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @PostMapping("/{id}/verify")
    public ResponseEntity<PaymentResponse> verifyBakongPayment(
            @PathVariable Long id) {
        Long userId = SecurityUtils.currentUserId()
                .orElseThrow(() ->
                        new UnauthorizedException("Authentication required"));

        PaymentResponse payment = paymentService.getPayment(id);

        if (payment.getOrderUserId() == null
                || !userId.equals(payment.getOrderUserId())) {
            throw new ForbiddenException(
                    "You don't have permission to verify this payment");
        }

        return ResponseEntity.ok(
                paymentService.verifyBakongPayment(id));
    }

    //Payment history by order
    @Operation(summary = "Get payment history for an order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment history retrieved successfully")
    })
    @GetMapping("/history/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentHistoryByOrder(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(
                paymentService.getPaymentHistoryByOrder(orderId));
    }

    //Payment history by user
    @Operation(summary = "Get payment history for a user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment history retrieved successfully")
    })
    @GetMapping("/history/user/{userId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentHistoryByUser(
            @PathVariable Long userId) {
        return ResponseEntity.ok(
                paymentService.getPaymentHistoryByUser(userId));
    }
}