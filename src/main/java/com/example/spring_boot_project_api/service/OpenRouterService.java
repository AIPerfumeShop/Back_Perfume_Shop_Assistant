package com.example.spring_boot_project_api.service;

import java.util.List;
import java.util.function.Consumer;

import com.example.spring_boot_project_api.dto.request.ai.AISearchPreferences;
import com.example.spring_boot_project_api.model.AIMessage;

public interface OpenRouterService {
    String generateResponse(List<AIMessage> messages, String productCatalog);

    void streamGenerateResponse(List<AIMessage> messages, String productCatalog, Consumer<String> onToken);

    /**
     * Ask the model to extract structured product search preferences from an
     * AI chat conversation. Never throws: on any failure an empty preference
     * set is returned so the caller can fall back to generic recommendations.
     */
    AISearchPreferences extractSearchPreferences(List<AIMessage> messages);
}