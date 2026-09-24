package com.example.spring_boot_project_api.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.spring_boot_project_api.config.BakongProperties;
import com.example.spring_boot_project_api.dto.request.bakong.BakongRequest;
import com.example.spring_boot_project_api.dto.request.bakong.CheckTransactionRequest;
import com.example.spring_boot_project_api.dto.request.payment.PaymentRequest;
import com.example.spring_boot_project_api.dto.response.bakong.BakongResponse;
import com.example.spring_boot_project_api.dto.response.payment.PaymentResponse;
import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.enums.PaymentMethod;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.exception.BakongException;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.PaymentMapper;
import com.example.spring_boot_project_api.model.AppSetting;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.repository.AppSettingRepository;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.service.BakongService;
import com.example.spring_boot_project_api.service.NotificationService;
import com.example.spring_boot_project_api.service.PaymentService;
import com.example.spring_boot_project_api.service.TelegramService;

import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentMapper paymentMapper;
    private final BakongService bakongService;
    private final TelegramService telegramService;
    private final BakongProperties bakongProperties;
    private final NotificationService notificationService;
    private final AppSettingRepository appSettingRepository;
    private final int maxDailyChecks;
    private final long minCheckIntervalMs;
    private final int forcedVerifyReserve;

    // Bakong limits check_transaction_by_md5 to 100 requests/day. The frontend
    // auto-polls the verify endpoint every few seconds, so without guarding the
    // upstream call itself the budget is exhausted by a single pending order.
    // Daily usage and the circuit-breaker are persisted in tb_app_settings so
    // an application restart cannot reset the budget mid-day.
    private static final String BAKONG_DAILY_CHECKS_PREFIX = "bakong.daily.checks.";
    private static final String BAKONG_DAILY_SUSPEND_KEY = "bakong.daily.suspend-until";

    private final Map<Long, Instant> lastBakongApiCheck = new ConcurrentHashMap<>();

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              OrderRepository orderRepository,
                              PaymentMapper paymentMapper,
                              BakongService bakongService,
                              TelegramService telegramService,
                              BakongProperties bakongProperties,
                              NotificationService notificationService,
                              AppSettingRepository appSettingRepository,
                              @Value("${payment.bakong.max-daily-checks:90}")
                              int maxDailyChecks,
                              @Value("${payment.bakong.min-check-interval-ms:180000}")
                              long minCheckIntervalMs,
                              @Value("${payment.bakong.forced-verify-reserve:10}")
                              int forcedVerifyReserve) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentMapper = paymentMapper;
        this.bakongService = bakongService;
        this.telegramService = telegramService;
        this.bakongProperties = bakongProperties;
        this.notificationService = notificationService;
        this.appSettingRepository = appSettingRepository;
        this.maxDailyChecks = maxDailyChecks;
        this.minCheckIntervalMs = minCheckIntervalMs;
        this.forcedVerifyReserve = Math.max(0, Math.min(forcedVerifyReserve, maxDailyChecks));
    }

    @Override
    public Payment initPayment(Order order, String paymentMethodName) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(resolvePaymentMethod(paymentMethodName));
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionId(UUID.randomUUID().toString());
        return paymentRepository.save(payment);
    }

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        PaymentMethod method = resolvePaymentMethod(request.getPaymentMethod());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found with ID : " + request.getOrderId()));

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(method);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionId(UUID.randomUUID().toString());

        return paymentMapper.toResponse(paymentRepository.save(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId) {
        return paymentMapper.toResponse(findPayment(paymentId));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new BadRequestException("Transaction ID is required");
        }
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found for transaction ID : " + transactionId));
        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatus(Long paymentId) {
        return paymentMapper.toResponse(findPayment(paymentId));
    }

    @Override
    public PaymentResponse validatePayment(Long paymentId) {
        Payment payment = findPayment(paymentId);

        if (payment.getStatus() == PaymentStatus.SUCCESSFUL) {
            throw new BadRequestException(
                    "Payment has already been completed");
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new BadRequestException(
                    "Payment has already been refunded");
        }
        if (payment.getPaymentMethod() == null) {
            throw new BadRequestException(
                    "Payment method is missing");
        }
        if (payment.getAmount() == null
                || payment.getAmount().signum() <= 0) {
            payment.setErrorMessage("Invalid payment amount");
            paymentRepository.save(payment);
            throw new BadRequestException(
                    "Payment amount must be positive and present");
        }
        if (payment.getOrder() == null) {
            payment.setErrorMessage("Payment is not linked to an order");
            paymentRepository.save(payment);
            throw new BadRequestException(
                    "Payment must be linked to an order");
        }
        if (payment.getOrder().getStatus() == null) {
            payment.setErrorMessage("Order status is missing");
            paymentRepository.save(payment);
            throw new BadRequestException(
                    "Order status is missing");
        }

        payment.setErrorMessage(null);
        paymentRepository.save(payment);
        return paymentMapper.toResponse(payment);
    }

    @Override
    public boolean processPayment(Long paymentId) {
        Payment payment = findPayment(paymentId);

        // Idempotent: a retry of an already-processed payment returns the same
        // outcome without re-processing, so double-taps cannot double-charge.
        if (payment.getStatus() == PaymentStatus.SUCCESSFUL) {
            return true;
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new BadRequestException(
                    "Payment cannot be processed again");
        }
        if (payment.getPaymentMethod() == PaymentMethod.CASH) {
            throw new BadRequestException(
                    "Cash on delivery is only settled when the order is delivered");
        }

        boolean success = payment.getAmount() != null
                && payment.getAmount().signum() > 0
                && payment.getPaymentMethod() != null
                && payment.getOrder() != null;

        PaymentStatus target = success
                ? PaymentStatus.SUCCESSFUL
                : PaymentStatus.FAILED;
        LocalDateTime paidAt = success ? LocalDateTime.now() : null;
        String errorMessage = success ? null : "Payment validation failed";

        // Only a PENDING payment may transition. If another request already
        // processed it, the update matches 0 rows and we just report the
        // current outcome instead of processing twice.
        int updated = paymentRepository.transitionFromPending(
                paymentId, target, errorMessage, paidAt);

        if (updated == 1) {
            // Keep the managed entity consistent for callers that read it
            // immediately afterwards (e.g. checkout response mapping).
            payment.setStatus(target);
            payment.setPaidAt(paidAt);
            payment.setErrorMessage(errorMessage);

            if (success) {
                sendPaymentNotificationAfterCommit(paymentMapper.toResponse(payment));
            }
            return success;
        }

        // A concurrent request already transitioned this payment: report the
        // terminal outcome idempotently.
        Payment current = findPayment(paymentId);
        if (current.getStatus() == PaymentStatus.SUCCESSFUL) {
            return true;
        }
        if (current.getStatus() == PaymentStatus.FAILED) {
            return false;
        }
        throw new BadRequestException(
                "Payment cannot be processed again");
    }

    @Override
    public Payment initBakongPayment(Order order) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(PaymentMethod.KHQR);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionId(UUID.randomUUID().toString());

        KHQRResponse<KHQRData> response = bakongService.generateQR(
                new BakongRequest(
                        null,
                        order.getTotalAmount().doubleValue(),
                        bakongProperties.getMerchantName(),
                        bakongProperties.getMerchantCity(),
                        bakongProperties.getMerchantId(),
                        bakongProperties.getAcquiringBank(),
                        null, null,
                        String.valueOf(order.getId()),
                        bakongProperties.getStoreLabel(),
                        bakongProperties.getTerminalLabel(),
                        null, null, null, null, null));

        if (response == null
                || response.getKHQRStatus() == null
                || response.getKHQRStatus().getCode() != 0
                || response.getData() == null
                || response.getData().getQr() == null) {

            String message = response != null
                    && response.getKHQRStatus() != null
                    ? response.getKHQRStatus().getMessage()
                    : "KHQR generation failed";
            throw new BakongException(
                    "Failed to generate KHQR code: " + message);
        }

        payment.setMd5(response.getData().getMd5());
        payment.setQrText(response.getData().getQr());

        return paymentRepository.save(payment);
    }

    @Override
    public PaymentResponse verifyBakongPayment(Long paymentId) {
        return verifyBakongPayment(paymentId, false);
    }

    @Override
    public PaymentResponse verifyBakongPayment(Long paymentId, boolean force) {
        Payment payment = findPayment(paymentId);

        // Already terminal: return current status idempotently instead of
        // throwing, so the frontend auto-poll can pick up the result.
        if (payment.getStatus() == PaymentStatus.SUCCESSFUL
                || payment.getStatus() == PaymentStatus.REFUNDED) {
            return paymentMapper.toResponse(payment);
        }
        if (payment.getMd5() == null || payment.getMd5().isBlank()) {
            throw new BadRequestException(
                    "Payment has no KHQR code attached");
        }

        if (bakongProperties.isConfigured()
                && !allowBakongCheck(paymentId, force)) {
            // Upstream budget exhausted or this payment was just checked:
            // answer with the stored status instead of calling Bakong again.
            return paymentMapper.toResponse(findPayment(paymentId));
        }

        log.info("Verifying payment {} with Bakong API (md5={})",
                paymentId, payment.getMd5() != null ? payment.getMd5().substring(0, Math.min(8, payment.getMd5().length())) + "..." : "null");

        BakongResponse bakongResponse = bakongService.checkTransactionByMD5(
                new CheckTransactionRequest(payment.getMd5()));

        log.info("Bakong API response for payment {}: success={}, responseCode={}, data={}",
                paymentId,
                bakongResponse != null ? bakongResponse.isSuccess() : "null",
                bakongResponse != null ? bakongResponse.responseCode() : "null",
                bakongResponse != null ? bakongResponse.data() : "null");

        if (isDailyLimitExceeded(bakongResponse)) {
            // Close the circuit for the rest of the day: further retries would
            // only burn requests that can never succeed. The circuit above
            // makes every later poll return quickly without hitting the API.
            suspendBakongChecks();
        }

        if (bakongResponse != null && bakongResponse.isSuccess()) {
            LocalDateTime paidAt = LocalDateTime.now();

            String externalRef = null;
            if (bakongResponse.data() instanceof Map<?, ?> dataMap
                    && dataMap.containsKey("externalRef")) {
                Object ref = dataMap.get("externalRef");
                externalRef = ref == null ? null : String.valueOf(ref);
            }

            // Atomic CAS: only one caller wins the PENDING -> SUCCESSFUL
            // transition. Concurrent callers get updated == 0 and skip
            // the notification / order update.
            int updated = paymentRepository.transitionFromPendingToSuccessful(
                    paymentId, PaymentStatus.SUCCESSFUL, paidAt, externalRef);

            if (updated == 1) {
                payment.setStatus(PaymentStatus.SUCCESSFUL);
                payment.setPaidAt(paidAt);
                payment.setExternalRef(externalRef);
                lastBakongApiCheck.remove(paymentId);

                Order order = payment.getOrder();
                if (order != null) {
                    OrderStatus oldStatus = order.getStatus();
                    order.setStatus(OrderStatus.PAID);
                    orderRepository.save(order);
                    notificationService.orderStatusChanged(order, oldStatus, OrderStatus.PAID, null);
                }

                sendPaymentNotificationAfterCommit(paymentMapper.toResponse(payment));
            }
            // If updated == 0, another caller already transitioned: no
            // duplicate Telegram or order update.
        }

        return paymentMapper.toResponse(findPayment(paymentId));
    }

    /**
     * Decides whether a Bakong upstream call is allowed now. Guards the
     * 100-requests/day budget and the per-payment polling cadence. A
     * {@code force} check (customer clicking "I have paid") bypasses the
     * per-payment interval so confirmation is immediate. Automatic checks
     * stop early so the last {@code forcedVerifyReserve} calls today are
     * reserved for real, customer-confirmed payments.
     */
    private boolean allowBakongCheck(Long paymentId, boolean force) {
        LocalDate today = LocalDate.now();

        if (isDailyCircuitOpen()) {
            return false;
        }

        if (!force) {
            Instant lastCheck = lastBakongApiCheck.get(paymentId);
            if (lastCheck != null
                    && Duration.between(lastCheck, Instant.now()).toMillis()
                            < minCheckIntervalMs) {
                return false;
            }
        }

        int usedToday = dailyChecksUsed(today);
        if (usedToday >= maxDailyChecks) {
            suspendBakongChecks();
            return false;
        }

        // Non-forced polling yields to paid-customer confirmations when only
        // the reserve remains for today.
        int autoQuota = Math.max(0, maxDailyChecks - forcedVerifyReserve);
        if (!force && usedToday >= autoQuota) {
            log.warn("Reached automatic check quota ({}/{}) for {}. "
                            + "Reserving the remaining {} checks today for "
                            + "customer-confirmed payments.",
                    usedToday, maxDailyChecks,
                    bakongProperties.isConfigured()
                            ? bakongProperties.getMerchantId() : "merchant",
                    forcedVerifyReserve);
            return false;
        }

        appSettingRepository.incrementCounter(BAKONG_DAILY_CHECKS_PREFIX + today);
        lastBakongApiCheck.put(paymentId, Instant.now());
        return true;
    }

    private int dailyChecksUsed(LocalDate day) {
        return appSettingRepository
                .findBySettingKey(BAKONG_DAILY_CHECKS_PREFIX + day)
                .map(s -> parseIntOrZero(s.getSettingValue()))
                .orElse(0);
    }

    private boolean isDailyCircuitOpen() {
        return appSettingRepository.findBySettingKey(BAKONG_DAILY_SUSPEND_KEY)
                .map(s -> LocalDate.now().equals(parseDateOrNull(s.getSettingValue())))
                .orElse(false);
    }

    private void suspendBakongChecks() {
        LocalDate today = LocalDate.now();
        AppSetting circuit = appSettingRepository
                .findBySettingKey(BAKONG_DAILY_SUSPEND_KEY)
                .orElseGet(AppSetting::new);
        circuit.setSettingKey(BAKONG_DAILY_SUSPEND_KEY);
        circuit.setSettingValue(today.toString());
        appSettingRepository.save(circuit);

        log.error("Bakong daily verification limit ({}) reached for {}. "
                        + "Suspending automatic KHQR checks until {}",
                maxDailyChecks, bakongProperties.isConfigured()
                        ? bakongProperties.getMerchantId() : "merchant",
                today.plusDays(1));
    }

    private static int parseIntOrZero(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static LocalDate parseDateOrNull(String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isDailyLimitExceeded(BakongResponse response) {
        if (response == null) {
            return false;
        }
        if (response.errorCode() != null && response.errorCode() == 17) {
            return true;
        }
        return response.responseMessage() != null
                && response.responseMessage().toLowerCase().contains("limit");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentHistoryByOrder(Long orderId) {
        return paymentMapper.toResponseList(
                paymentRepository.findAllByOrderIdOrderByCreatedAtDesc(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentHistoryByUser(Long userId) {
        return paymentMapper.toResponseList(
                paymentRepository.findAllByOrderUserIdOrderByCreatedAtDesc(userId));
    }

    private Payment findPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found with ID : " + paymentId));
    }

    /**
     * Sends the Telegram payment notification only after the current
     * transaction commits, so a failed commit (e.g. a DB constraint error)
     * never announces a payment that was not actually persisted. When called
     * outside a transaction, it sends immediately.
     */
    private void sendPaymentNotificationAfterCommit(PaymentResponse payment) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status == STATUS_COMMITTED) {
                                telegramService.sendPaymentNotification(payment);
                            }
                        }
                    });
        } else {
            telegramService.sendPaymentNotification(payment);
        }
    }

    private PaymentMethod resolvePaymentMethod(String paymentMethodName) {
        if (paymentMethodName == null || paymentMethodName.isBlank()) {
            throw new BadRequestException("Payment method is required");
        }
        try {
            return PaymentMethod.valueOf(paymentMethodName.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    "Invalid payment method : " + paymentMethodName
                            + ". Allowed values : " + String.join(", ",
                            java.util.Arrays.stream(PaymentMethod.values())
                                    .map(Enum::name)
                                    .toList()));
        }
    }

    @Override
    public boolean expirePayment(Long paymentId) {
        Payment payment = findPayment(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return false;
        }

        int updated = paymentRepository.transitionFromPending(
                paymentId, PaymentStatus.FAILED,
                "Payment expired while awaiting KHQR transfer", null);
        if (updated == 1) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage(
                    "Payment expired while awaiting KHQR transfer");
            return true;
        }

        //A concurrent verification settled the payment first.
        return false;
    }

    @Override
    public void announceSuccessfulPayment(Long paymentId) {
        Payment payment = findPayment(paymentId);
        if (payment.getStatus() == PaymentStatus.SUCCESSFUL) {
            sendPaymentNotificationAfterCommit(paymentMapper.toResponse(payment));
        }
    }

    @Override
    public PaymentResponse updatePaymentStatus(Long paymentId, String status, String reason) {
        Payment payment = findPayment(paymentId);

        PaymentStatus newStatus;
        try {
            newStatus = PaymentStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    "Invalid payment status: " + status
                            + ". Allowed values: SUCCESSFUL, FAILED, REFUNDED");
        }

        if (newStatus != PaymentStatus.SUCCESSFUL
                && newStatus != PaymentStatus.FAILED
                && newStatus != PaymentStatus.REFUNDED) {
            throw new BadRequestException(
                    "Only terminal statuses (SUCCESSFUL, FAILED, REFUNDED) can be set manually");
        }

        PaymentStatus oldStatus = payment.getStatus();
        if (oldStatus == newStatus) {
            return paymentMapper.toResponse(payment);
        }

        payment.setStatus(newStatus);
        if (newStatus == PaymentStatus.SUCCESSFUL) {
            payment.setPaidAt(LocalDateTime.now());
            payment.setErrorMessage(null);

            Order order = payment.getOrder();
            if (order != null && order.getStatus() != OrderStatus.PAID) {
                OrderStatus orderOldStatus = order.getStatus();
                order.setStatus(OrderStatus.PAID);
                orderRepository.save(order);
                notificationService.orderStatusChanged(order, orderOldStatus, OrderStatus.PAID,
                        reason != null ? reason : "Payment confirmed by admin");
            }
        } else {
            payment.setErrorMessage(reason);
        }

        paymentRepository.save(payment);

        log.info("Admin updated payment {} status: {} -> {} (reason: {})",
                paymentId, oldStatus, newStatus, reason);

        return paymentMapper.toResponse(findPayment(paymentId));
    }
}