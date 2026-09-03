package com.example.spring_boot_project_api.dto.response.order;

import com.example.spring_boot_project_api.enums.PaymentMethod;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class CheckoutResponse {
    private Long orderId;
    private BigDecimal totalAmount;
    private String orderStatus;
    private Long paymentId;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String transactionId;
}
