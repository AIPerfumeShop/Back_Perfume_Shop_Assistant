package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.payment.PaymentRequest;
import com.example.spring_boot_project_api.dto.response.payment.PaymentResponse;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.Payment;

public interface PaymentService {

    /**
     * Initialize a payment record for an order (used by order checkout).
     */
    Payment initPayment(Order order, String paymentMethodName);

    /**
     * Create a new payment for an existing order.
     */
    PaymentResponse createPayment(PaymentRequest request);

    /**
     * Get a single payment by id.
     */
    PaymentResponse getPayment(Long paymentId);

    /**
     * Get a single payment by its transaction id.
     */
    PaymentResponse getPaymentByTransactionId(String transactionId);

    /**
     * Get the current status of a payment.
     */
    PaymentResponse getPaymentStatus(Long paymentId);

    /**
     * Validate a payment (method, amount, order) before it is processed.
     */
    PaymentResponse validatePayment(Long paymentId);

    /**
     * Process (simulate gateway) a payment and update its status.
     */
    boolean processPayment(Long paymentId);

    /**
     * Create a PENDING payment and generate a KHQR QR code via Bakong.
     */
    Payment initBakongPayment(Order order);

    /**
     * Check a KHQR payment's status against Bakong and, if paid,
     * mark both the payment as SUCCESSFUL and the order as PAID.
     */
    PaymentResponse verifyBakongPayment(Long paymentId);

    /**
     * List the payment history for a given order.
     */
    List<PaymentResponse> getPaymentHistoryByOrder(Long orderId);

    /**
     * List the payment history for a given user.
     */
    List<PaymentResponse> getPaymentHistoryByUser(Long userId);
}
