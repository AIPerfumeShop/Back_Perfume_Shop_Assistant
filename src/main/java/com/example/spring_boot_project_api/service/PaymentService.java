package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.Payment;

public interface PaymentService {
    Payment initPayment(Order order, String paymentMethodName);

    boolean processPayment(Long paymentId);
}
