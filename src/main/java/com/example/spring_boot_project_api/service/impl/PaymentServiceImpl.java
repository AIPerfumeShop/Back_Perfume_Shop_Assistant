package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.spring_boot_project_api.enums.PaymentMethod;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.service.PaymentService;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Payment initPayment(Order order, String paymentMethodName) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(PaymentMethod.valueOf(paymentMethodName));
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionId(UUID.randomUUID().toString());
        return paymentRepository.save(payment);
    }

    @Override
    public boolean processPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found"));

        //Simulate gateway decision: payment fails when amount is not positive
        boolean success = payment.getAmount() != null
                && payment.getAmount().signum() > 0;

        payment.setStatus(success
                ? PaymentStatus.SUCCESSFUL
                : PaymentStatus.FAILED);

        if (success) {
            payment.setPaidAt(LocalDateTime.now());
        }

        paymentRepository.save(payment);

        return success;
    }
}
