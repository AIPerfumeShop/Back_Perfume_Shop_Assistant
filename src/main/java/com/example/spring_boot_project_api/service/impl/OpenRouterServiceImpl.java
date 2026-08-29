package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.example.spring_boot_project_api.dto.external.openrouter.OpenRouterMessage;
import com.example.spring_boot_project_api.dto.external.openrouter.OpenRouterRequest;
import com.example.spring_boot_project_api.dto.external.openrouter.OpenRouterResponse;
import com.example.spring_boot_project_api.enums.MessageSender;
import com.example.spring_boot_project_api.model.AIMessage;
import com.example.spring_boot_project_api.service.OpenRouterService;

@Service
public class OpenRouterServiceImpl implements OpenRouterService {

    private final RestClient restClient;

    private final String apiKey;

    private final String model;

    public OpenRouterServiceImpl(
            @Value("${openrouter.url}") String url,
            @Value("${openrouter.api-key}") String apiKey,
            @Value("${openrouter.model}") String model) {

        this.apiKey = apiKey;
        this.model = model;

        this.restClient = RestClient.builder()
                .baseUrl(url)
                .build();
    }

    @Override
    public String generateResponse(List<AIMessage> messages) {

        // OpenRouter API logic will be added here later
        // Convert AIMessage -> OpenRouterMessage
        List<OpenRouterMessage> openRouterMessages =
                messages.stream()
                        .map(message -> {

                            String role =
                                    message.getSender() == MessageSender.USER
                                            ? "user"
                                            : "assistant";

                            return new OpenRouterMessage(
                                    role,
                                    message.getMessage()
                            );
                        })
                        .toList();
        // Create OpenRouter request
        OpenRouterRequest request =
                new OpenRouterRequest(
                        model,
                        openRouterMessages
                );
        OpenRouterResponse response = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization","Bearer " + apiKey)
                .body(request)
                .retrieve()
                .body(OpenRouterResponse.class);
        if(response == null 
            || response.getChoices() == null 
            || response.getChoices().isEmpty() 
            || response.getChoices().get(0).getMessage() == null){
                throw new RuntimeException("No response received from OpenRouter");
        }
        return response.getChoices()
        .get(0)
        .getMessage()
        .getContent();
    }
}
