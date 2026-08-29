package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.model.AIMessage;

public interface OpenRouterService {
    String generateResponse(List<AIMessage> messages);
}
