package com.example.spring_boot_project_api.service;

import java.util.List;
import java.util.function.Consumer;

import com.example.spring_boot_project_api.model.AIMessage;

public interface OpenRouterService {
    String generateResponse(List<AIMessage> messages);

    void streamGenerateResponse(List<AIMessage> messages, Consumer<String> onToken);
}