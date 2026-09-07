package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.payment.PaymentRequest;
import com.example.spring_boot_project_api.dto.response.payment.PaymentResponse;
import com.example.spring_boot_project_api.enums.PaymentMethod;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.PaymentMapper;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.repository.OrderRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.service.PaymentService;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentMapper paymentMapper;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              OrderRepository orderRepository,
                              PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentMapper = paymentMapper;
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

        //Payment validation before processing
        if (payment.getStatus() == PaymentStatus.SUCCESSFUL
                || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new BadRequestException(
                    "Payment cannot be processed again");
        }

        boolean success = payment.getAmount() != null
                && payment.getAmount().signum() > 0
                && payment.getPaymentMethod() != null
                && payment.getOrder() != null;

        payment.setStatus(success
                ? PaymentStatus.SUCCESSFUL
                : PaymentStatus.FAILED);
        payment.setErrorMessage(success ? null : "Payment validation failed");

        if (success) {
            payment.setPaidAt(LocalDateTime.now());
        }

        paymentRepository.save(payment);

        return success;
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