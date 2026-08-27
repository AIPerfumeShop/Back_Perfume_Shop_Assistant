package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.ai.AIChatRequest;
import com.example.spring_boot_project_api.dto.response.ai.AIChatResponse;
import com.example.spring_boot_project_api.service.AIService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ai")
public class AIController {
    private final AIService aiService;
    public AIController(AIService aiService){
        this.aiService = aiService;
    }
    @PostMapping("/chat")
    public ResponseEntity<AIChatResponse> chat(@RequestParam Long userId, @Valid @RequestBody AIChatRequest request){
        AIChatResponse response = aiService.chat(userId, request);
        return ResponseEntity.ok(response);
    }
}
