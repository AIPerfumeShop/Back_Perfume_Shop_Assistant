package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.response.order.OrderResponse;

public interface TelegramService {

    boolean isEnabled();

    void sendMessage(String text);

    void sendOrderNotification(OrderResponse order);

    void sendRecommendationSummary(String userName, List<String> productLines);
}