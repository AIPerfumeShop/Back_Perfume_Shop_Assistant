package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.response.order.OrderResponse;
import com.example.spring_boot_project_api.dto.response.payment.PaymentResponse;

public interface TelegramService {

    boolean isEnabled();

    void sendMessage(String text);

    void sendOrderNotification(OrderResponse order);

    void sendPaymentNotification(PaymentResponse payment);
}