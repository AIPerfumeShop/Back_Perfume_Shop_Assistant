package com.example.spring_boot_project_api.dto.response.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.PaymentMethod;
import com.example.spring_boot_project_api.enums.PaymentStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private Long orderUserId;
    private PaymentMethod paymentMethod;
    private String transactionId;
    private BigDecimal amount;
    private PaymentStatus status;
    private LocalDateTime paidAt;
    private String errorMessage;
    private String qrText;
    private String md5;
    private String externalRef;
    private LocalDateTime createdAt;
}
