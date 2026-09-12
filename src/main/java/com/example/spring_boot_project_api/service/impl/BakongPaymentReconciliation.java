package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import com.example.spring_boot_project_api.config.BakongProperties;
import com.example.spring_boot_project_api.dto.response.payment.PaymentResponse;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.BakongException;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.service.OrderService;
import com.example.spring_boot_project_api.service.PaymentService;

@Component
public class BakongPaymentReconciliation {

    private static final Logger log = LoggerFactory.getLogger(
            BakongPaymentReconciliation.class);

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final BakongProperties bakongProperties;
    private final long expiryMinutes;

    public BakongPaymentReconciliation(PaymentRepository paymentRepository,
                                       PaymentService paymentService,
                                       OrderService orderService,
                                       BakongProperties bakongProperties,
                                       @Value("${payment.bakong.payment-expiry-minutes:15}")
                                       long expiryMinutes) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.bakongProperties = bakongProperties;
        this.expiryMinutes = expiryMinutes;
    }

    @Scheduled(
            fixedDelayString = "${payment.bakong.poll-interval-ms:15000}",
            initialDelayString = "${payment.bakong.initial-delay-ms:30000}")
    public void reconcile() {
        if (!bakongProperties.isConfigured()) {
            log.debug("Bakong not configured; skipping KHQR reconciliation");
            return;
        }

        List<Payment> pending = paymentRepository
                .findAllByStatusAndMd5IsNotNull(PaymentStatus.PENDING);

        for (Payment payment : pending) {
            try {
                PaymentResponse verified = paymentService
                        .verifyBakongPayment(payment.getId());

                if (verified.getStatus() == PaymentStatus.SUCCESSFUL) {
                    continue;
                }

                if (isExpired(payment)) {
                    expire(payment);
                }
            } catch (BakongException
                     | BadRequestException
                     | RestClientException ex) {
                log.warn("KHQR verification failed for payment {}: {}",
                        payment.getId(), ex.getMessage());
            }
        }
    }

    private boolean isExpired(Payment payment) {
        return payment.getCreatedAt() != null
                && payment.getCreatedAt().isBefore(
                        LocalDateTime.now().minusMinutes(expiryMinutes));
    }

    private void expire(Payment payment) {
        Long orderId = payment.getOrder() != null
                ? payment.getOrder().getId()
                : null;

        payment.setStatus(PaymentStatus.FAILED);
        payment.setErrorMessage("Payment expired while awaiting KHQR transfer");
        paymentRepository.save(payment);

        if (orderId != null) {
            orderService.cancelOrderAdmin(
                    orderId, "Payment expired while awaiting KHQR transfer");
        }

        log.info("Expired unpaid KHQR payment {} for order {}",
                payment.getId(), orderId);
    }
}