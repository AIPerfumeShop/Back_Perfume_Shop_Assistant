package com.example.spring_boot_project_api.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

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
import com.example.spring_boot_project_api.exception.InvalidOrderException;
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
    private final DataSource dataSource;
    private final long expiryMinutes;
    private final long minCheckIntervalMs;
    private final Map<Long, Instant> lastChecked = new ConcurrentHashMap<>();

    // Cross-instance MySQL advisory lock so only one backend instance runs the
    // KHQR reconciliation at a time. Prevents two instances from duplicating
    // Bakong API checks (burning the daily budget) and redundant notifications.
    private static final String RECONCILE_LOCK = "bakong_payment_reconciliation";

    public BakongPaymentReconciliation(PaymentRepository paymentRepository,
                                       PaymentService paymentService,
                                       OrderService orderService,
                                       BakongProperties bakongProperties,
                                       DataSource dataSource,
                                       @Value("${payment.bakong.payment-expiry-minutes:15}")
                                       long expiryMinutes,
@Value("${payment.bakong.min-check-interval-ms:180000}")
                                        long minCheckIntervalMs) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.bakongProperties = bakongProperties;
        this.dataSource = dataSource;
        this.expiryMinutes = expiryMinutes;
        this.minCheckIntervalMs = minCheckIntervalMs;
    }

    @Scheduled(
            fixedDelayString = "${payment.bakong.poll-interval-ms:8000}",
            initialDelayString = "${payment.bakong.initial-delay-ms:5000}")
    public void reconcile() {
        if (!bakongProperties.isConfigured()) {
            log.debug("Bakong not configured; skipping KHQR reconciliation");
            return;
        }

        // GET_LOCK is connection-scoped, so acquire and release on the same
        // connection. Holding the lock for the duration of the tick keeps a
        // second backend instance from polling the same payments meanwhile.
        try (Connection connection = dataSource.getConnection()) {
            if (!acquireLock(connection)) {
                log.debug("Another instance holds the reconciliation lock; "
                        + "skipping this tick");
                return;
            }
            try {
                reconcilePendingPayments();
            } finally {
                releaseLock(connection);
            }
        } catch (SQLException e) {
            log.warn("Could not acquire reconciliation lock; skipping tick: {}",
                    e.getMessage());
        }
    }

    private void reconcilePendingPayments() {
        List<Payment> pending = paymentRepository
                .findAllByStatusAndMd5IsNotNull(PaymentStatus.PENDING);

        log.debug("KHQR reconciliation: checking {} pending payment(s)", pending.size());

        for (Payment payment : pending) {
            try {
                // Bakong has a daily API budget (100 checks/day).
                // Never hit it more than once per payment per throttle window.
                Instant checkedAt = lastChecked.get(payment.getId());
                if (checkedAt != null
                        && Duration.between(checkedAt, Instant.now()).toMillis()
                                < minCheckIntervalMs) {
                    continue;
                }
                lastChecked.put(payment.getId(), Instant.now());

                log.debug("Verifying payment {} (order {}, md5={})",
                        payment.getId(),
                        payment.getOrder() != null ? payment.getOrder().getId() : "null",
                        payment.getMd5() != null ? payment.getMd5().substring(0, Math.min(8, payment.getMd5().length())) + "..." : "null");

                PaymentResponse verified = paymentService
                        .verifyBakongPayment(payment.getId());

                if (verified.getStatus() == PaymentStatus.SUCCESSFUL) {
                    lastChecked.remove(payment.getId());
                    log.info("Payment {} confirmed as SUCCESSFUL", payment.getId());
                    continue;
                }

                if (isExpired(payment)) {
                    expire(payment);
                }
            } catch (BakongException
                     | BadRequestException
                     | InvalidOrderException
                     | RestClientException ex) {
                log.warn("KHQR verification failed for payment {}: {}",
                        payment.getId(), ex.getMessage(), ex);
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

        lastChecked.remove(payment.getId());

        //Atomic PENDING -> FAILED: if a concurrent verification already
        //settled this payment as SUCCESSFUL, the transition matches 0 rows
        //and we must not cancel the (now paid) order.
        if (!paymentService.expirePayment(payment.getId())) {
            log.info("Payment {} settled before expiry; skipping cancel",
                    payment.getId());
            return;
        }

        if (orderId != null) {
            orderService.cancelOrderAdmin(
                    orderId, "Payment expired while awaiting KHQR transfer");
        }

        log.info("Expired unpaid KHQR payment {} for order {}",
                payment.getId(), orderId);
    }

    private boolean acquireLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT GET_LOCK(?, 0)")) {
            statement.setString(1, RECONCILE_LOCK);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) == 1;
            }
        }
    }

    private void releaseLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, RECONCILE_LOCK);
            statement.executeQuery();
        }
    }
}