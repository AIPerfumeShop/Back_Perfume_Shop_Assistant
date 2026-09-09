package com.example.spring_boot_project_api.service.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.example.spring_boot_project_api.dto.external.openrouter.OpenRouterMessage;
import com.example.spring_boot_project_api.dto.external.openrouter.OpenRouterRequest;
import com.example.spring_boot_project_api.dto.external.openrouter.OpenRouterResponse;
import com.example.spring_boot_project_api.dto.external.openrouter.OpenRouterStreamChunk;
import com.example.spring_boot_project_api.enums.MessageSender;
import com.example.spring_boot_project_api.exception.AIServiceException;
import com.example.spring_boot_project_api.model.AIMessage;
import com.example.spring_boot_project_api.service.OpenRouterService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OpenRouterServiceImpl implements OpenRouterService {

    private static final String DATA_PREFIX = "data:";
    private static final String DONE = "[DONE]";

    private static final Logger log = LoggerFactory.getLogger(OpenRouterServiceImpl.class);

    private final RestClient restClient;

    private final String apiKey;

    private final String model;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        try {
            // Convert AIMessage -> OpenRouterMessage
            List<OpenRouterMessage> openRouterMessages = messages.stream()
                    .map(message -> {

                        String role = message.getSender() == MessageSender.USER
                                ? "user"
                                : "assistant";

                        return new OpenRouterMessage(
                                role,
                                message.getMessage());
                    })
                    .toList();

            // Create OpenRouter request
            OpenRouterRequest request = new OpenRouterRequest(
                    model,
                    openRouterMessages);

            // Send request to OpenRouter
            OpenRouterResponse response = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(OpenRouterResponse.class);

            // Check response
            if (response == null
                    || response.getChoices() == null
                    || response.getChoices().isEmpty()
                    || response.getChoices().get(0).getMessage() == null
                    || response.getChoices().get(0).getMessage().getContent() == null) {

                throw new AIServiceException(
                        "No response received from OpenRouter");
            }

            // Return AI response
            return response.getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

        } catch (AIServiceException ex) {

            // Keep our own AI exception
            throw ex;

        } catch (Exception ex) {

            // Convert OpenRouter errors into our AI exception
            log.error("OpenRouter non-stream call failed", ex);
            throw new AIServiceException(
                    "Failed to communicate with OpenRouter", ex);
        }
    }

    @Override
    public void streamGenerateResponse(List<AIMessage> messages, Consumer<String> onToken) {
        try {
            List<OpenRouterMessage> openRouterMessages = messages.stream()
                    .map(message -> {
                        String role = message.getSender() == MessageSender.USER
                                ? "user"
                                : "assistant";
                        return new OpenRouterMessage(role, message.getMessage());
                    })
                    .toList();

            OpenRouterRequest request = new OpenRouterRequest(model, openRouterMessages, true);

            restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(request)
                    .exchange((httpRequest, httpResponse) -> {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(httpResponse.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.startsWith(DATA_PREFIX)) {
                                    continue;
                                }
                                String data = line.substring(DATA_PREFIX.length()).trim();
                                if (data.isEmpty()) {
                                    continue;
                                }
                                if (DONE.equals(data)) {
                                    break;
                                }
                                OpenRouterStreamChunk chunk =
                                        objectMapper.readValue(data, OpenRouterStreamChunk.class);
                                String content = (chunk.getChoices() == null
                                        || chunk.getChoices().isEmpty()
                                        || chunk.getChoices().get(0).getDelta() == null)
                                                ? null
                                                : chunk.getChoices().get(0).getDelta().getContent();
                                if (content != null && !content.isEmpty()) {
                                    onToken.accept(content);
                                }
                            }
                        } catch (Exception ex) {
                            log.error("Failed to read OpenRouter stream", ex);
                            throw new AIServiceException("Failed to read OpenRouter stream", ex);
                        }
                        return null;
                    });

        } catch (AIServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("OpenRouter stream call failed", ex);
            throw new AIServiceException("Failed to stream response from OpenRouter", ex);
        }
    }
}