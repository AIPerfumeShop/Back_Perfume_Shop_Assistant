package com.example.spring_boot_project_api.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.payment.PaymentResponse;
import com.example.spring_boot_project_api.model.Payment;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        if (payment.getOrder() != null) {
            response.setOrderId(payment.getOrder().getId());
            if (payment.getOrder().getUser() != null) {
                response.setOrderUserId(
                        payment.getOrder().getUser().getId());
            }
        }
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setTransactionId(payment.getTransactionId());
        response.setAmount(payment.getAmount());
        response.setStatus(payment.getStatus());
        response.setPaidAt(payment.getPaidAt());
        response.setErrorMessage(payment.getErrorMessage());
        response.setQrText(payment.getQrText());
        response.setMd5(payment.getMd5());
        response.setExternalRef(payment.getExternalRef());
        response.setCreatedAt(payment.getCreatedAt());
        return response;
    }

    public List<PaymentResponse> toResponseList(List<Payment> payments) {
        return payments.stream()
                .map(this::toResponse)
                .toList();
    }
}
