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
     * Like {@link #verifyBakongPayment(Long)} but with an explicit
     * {@code force} flag: a forced check (customer clicked "I have paid")
     * bypasses the per-payment polling interval so confirmation is
     * immediate. The daily Bakong budget still applies.
     */
    PaymentResponse verifyBakongPayment(Long paymentId, boolean force);

    /** Force a KHQR status check in an independent transaction before cancellation. */
    PaymentResponse verifyBakongPaymentForCancellation(Long paymentId);

    /**
     * List the payment history for a given order.
     */
    List<PaymentResponse> getPaymentHistoryByOrder(Long orderId);

    /**
     * List the payment history for a given user.
     */
    List<PaymentResponse> getPaymentHistoryByUser(Long userId);

    /**
     * Admin: manually update a payment's status (e.g. mark as SUCCESSFUL or FAILED).
     */
    PaymentResponse updatePaymentStatus(Long paymentId, String status, String reason);

    /**
     * Atomically expire a PENDING (KHQR) payment as FAILED. Returns true only
     * if this call actually performed the transition (a concurrent verification
     * that already settled the payment causes a false return).
     */
    boolean expirePayment(Long paymentId);

    /**
     * Whether automatic Bakong verification is currently suspended (daily
     * quota exhausted or the circuit breaker is open). While suspended the
     * reconciliation must not expire/cancel payments based on a stored
     * PENDING status, because no upstream confirmation was actually received —
     * the customer may have paid and the shop simply cannot check.
     */
    boolean isBakongVerificationSuspended();

    /**
     * Send the "Payment Received" Telegram notification for a payment that has
     * just become SUCCESSFUL, but only after the current transaction commits.
     * Used by the CASH-on-delivery flow, which settles its payment directly
     * (outside {@link #processPayment}) and would otherwise send no income alert.
     */
    void announceSuccessfulPayment(Long paymentId);
}
