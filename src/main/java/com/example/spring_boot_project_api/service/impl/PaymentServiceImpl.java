package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.payment.PaymentRequest;
import com.example.spring_boot_project_api.dto.response.payment.PaymentResponse;
import com.example.spring_boot_project_api.config.BakongProperties;
import com.example.spring_boot_project_api.dto.request.bakong.CheckTransactionRequest;
import com.example.spring_boot_project_api.dto.request.bakong.BakongRequest;
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
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.service.BakongService;
import com.example.spring_boot_project_api.service.PaymentService;
import com.example.spring_boot_project_api.service.TelegramService;

import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentMapper paymentMapper;
    private final BakongService bakongService;
    private final TelegramService telegramService;
    private final BakongProperties bakongProperties;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              OrderRepository orderRepository,
                              PaymentMapper paymentMapper,
                              BakongService bakongService,
                              TelegramService telegramService,
                              BakongProperties bakongProperties) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentMapper = paymentMapper;
        this.bakongService = bakongService;
        this.telegramService = telegramService;
        this.bakongProperties = bakongProperties;
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
                telegramService.sendPaymentNotification(
                        paymentMapper.toResponse(payment));
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
        Payment payment = findPayment(paymentId);

        if (payment.getStatus() == PaymentStatus.SUCCESSFUL
                || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new BadRequestException(
                    "Payment is already in terminal state");
        }
        if (payment.getMd5() == null || payment.getMd5().isBlank()) {
            throw new BadRequestException(
                    "Payment has no KHQR code attached");
        }

        BakongResponse bakongResponse = bakongService.checkTransactionByMD5(
                new CheckTransactionRequest(payment.getMd5()));

        if (bakongResponse != null && bakongResponse.isSuccess()) {
            payment.setStatus(PaymentStatus.SUCCESSFUL);
            payment.setPaidAt(LocalDateTime.now());

            if (bakongResponse.data() instanceof Map<?, ?> dataMap
                    && dataMap.containsKey("externalRef")) {
                Object ref = dataMap.get("externalRef");
                payment.setExternalRef(
                        ref == null ? null : String.valueOf(ref));
            }

            Order order = payment.getOrder();
            if (order != null) {
                order.setStatus(OrderStatus.PAID);
                orderRepository.save(order);
            }

            paymentRepository.save(payment);
            telegramService.sendPaymentNotification(
                    paymentMapper.toResponse(payment));
        } else {
            paymentRepository.save(payment);
        }

        return paymentMapper.toResponse(payment);
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
}