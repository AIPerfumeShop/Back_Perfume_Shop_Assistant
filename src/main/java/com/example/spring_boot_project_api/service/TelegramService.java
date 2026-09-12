package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.response.order.OrderResponse;

public interface TelegramService {

    boolean isEnabled();

    void sendMessage(String text);

    void sendOrderNotification(OrderResponse order);
}