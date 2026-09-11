package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.spring_boot_project_api.config.BakongProperties;
import com.example.spring_boot_project_api.dto.response.payment.PaymentResponse;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.exception.BakongException;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.service.OrderService;
import com.example.spring_boot_project_api.service.PaymentService;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class BakongPaymentReconciliationTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private OrderService orderService;
    @Mock
    private BakongProperties bakongProperties;

    private BakongPaymentReconciliation reconciliation;

    @BeforeEach
    void setUp() {
        reconciliation = new BakongPaymentReconciliation(
                paymentRepository, paymentService, orderService,
                bakongProperties, 15L);
    }

    @Test
    void reconcile_skipsWhenBakongNotConfigured() {
        when(bakongProperties.isConfigured()).thenReturn(false);

        reconciliation.reconcile();

        verify(paymentRepository, never())
                .findAllByStatusAndMd5IsNotNull(any());
        verify(paymentService, never()).verifyBakongPayment(anyLong());
    }

    @Test
    void reconcile_expiresUnpaidPaymentPastThreshold() {
        Payment payment = pendingPayment(10L, 20);
        PaymentResponse pending = new PaymentResponse();
        pending.setStatus(PaymentStatus.PENDING);

        when(bakongProperties.isConfigured()).thenReturn(true);
        when(paymentRepository.findAllByStatusAndMd5IsNotNull(
                PaymentStatus.PENDING)).thenReturn(List.of(payment));
        when(paymentService.verifyBakongPayment(1L)).thenReturn(pending);

        reconciliation.reconcile();

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(paymentRepository).save(payment);
        verify(orderService).cancelOrderAdmin(
                eq(10L), anyString());
    }

    @Test
    void reconcile_keepsRecentUnpaidPaymentPending() {
        Payment payment = pendingPayment(10L, 5);
        PaymentResponse pending = new PaymentResponse();
        pending.setStatus(PaymentStatus.PENDING);

        when(bakongProperties.isConfigured()).thenReturn(true);
        when(paymentRepository.findAllByStatusAndMd5IsNotNull(
                PaymentStatus.PENDING)).thenReturn(List.of(payment));
        when(paymentService.verifyBakongPayment(1L)).thenReturn(pending);

        reconciliation.reconcile();

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderService, never()).cancelOrderAdmin(anyLong(), anyString());
    }

    @Test
    void reconcile_skipsExpiryWhenPaymentIsSuccessful() {
        Payment payment = pendingPayment(10L, 20);
        PaymentResponse successful = new PaymentResponse();
        successful.setStatus(PaymentStatus.SUCCESSFUL);

        when(bakongProperties.isConfigured()).thenReturn(true);
        when(paymentRepository.findAllByStatusAndMd5IsNotNull(
                PaymentStatus.PENDING)).thenReturn(List.of(payment));
        when(paymentService.verifyBakongPayment(1L)).thenReturn(successful);

        reconciliation.reconcile();

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verify(orderService, never()).cancelOrderAdmin(anyLong(), anyString());
    }

    @Test
    void reconcile_keepsPaymentPendingWhenVerifyFails() {
        Payment payment = pendingPayment(10L, 20);

        when(bakongProperties.isConfigured()).thenReturn(true);
        when(paymentRepository.findAllByStatusAndMd5IsNotNull(
                PaymentStatus.PENDING)).thenReturn(List.of(payment));
        when(paymentService.verifyBakongPayment(1L))
                .thenThrow(new BakongException("Bakong API unavailable"));

        reconciliation.reconcile();

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verify(orderService, never()).cancelOrderAdmin(anyLong(), anyString());
    }

    private Payment pendingPayment(long orderId, long createdAtMinutesAgo) {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMd5("abc123");
        payment.setCreatedAt(LocalDateTime.now()
                .minusMinutes(createdAtMinutesAgo));

        Order order = new Order();
        order.setId(orderId);
        payment.setOrder(order);
        return payment;
    }
}