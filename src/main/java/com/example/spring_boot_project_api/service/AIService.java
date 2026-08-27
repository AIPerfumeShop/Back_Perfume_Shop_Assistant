package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.ai.AIChatRequest;
import com.example.spring_boot_project_api.dto.response.ai.AIChatResponse;

public interface AIService {
    //send a message and receive an AI response
    AIChatResponse chat(Long userId, AIChatRequest request);
}
