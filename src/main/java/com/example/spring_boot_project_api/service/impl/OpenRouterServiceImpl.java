package com.example.spring_boot_project_api.service.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import com.example.spring_boot_project_api.dto.request.ai.AISearchPreferences;
import com.example.spring_boot_project_api.enums.Gender;
import com.example.spring_boot_project_api.enums.MessageSender;
import com.example.spring_boot_project_api.exception.AIServiceException;
import com.example.spring_boot_project_api.model.AIMessage;
import com.example.spring_boot_project_api.service.OpenRouterService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OpenRouterServiceImpl implements OpenRouterService {

    private static final String DATA_PREFIX = "data:";
    private static final String DONE = "[DONE]";

    private static final Logger log = LoggerFactory.getLogger(OpenRouterServiceImpl.class);

    private final RestClient restClient;

    private final String apiKey;

    private final String model;

    private final String systemPrompt;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenRouterServiceImpl(
            @Value("${openrouter.url}") String url,
            @Value("${openrouter.api-key}") String apiKey,
            @Value("${openrouter.model}") String model,
            @Value("${ai.system-prompt:"
                    + "You are the friendly assistant of Blossom Fragrance perfume shop. "
                    + "Recommend only perfumes that exist in the shop catalog. "
                    + "Keep answers SHORT and easy to skim: at most 3-6 short bullet points "
                    + "or 2-4 sentences. Never write long essays, huge tables, or long "
                    + "introductions. Skip closing questions like 'what would you like next?' "
                    + "unless the customer asks for help narrowing down.}") String systemPrompt) {

        this.apiKey = apiKey;
        this.model = model;
        this.systemPrompt = systemPrompt;

        this.restClient = RestClient.builder()
                .baseUrl(url)
                .build();
    }

    private List<OpenRouterMessage> withSystemPrompt(String productCatalog, List<OpenRouterMessage> messages) {
        List<OpenRouterMessage> all = new ArrayList<>(messages.size() + 2);
        all.add(new OpenRouterMessage("system", systemPrompt));
        if (productCatalog != null && !productCatalog.isBlank()) {
            all.add(new OpenRouterMessage("system", productCatalog));
        }
        all.addAll(messages);
        return all;
    }

    @Override
    public String generateResponse(List<AIMessage> messages, String productCatalog) {
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
                    withSystemPrompt(productCatalog, openRouterMessages));

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
    public void streamGenerateResponse(List<AIMessage> messages, String productCatalog, Consumer<String> onToken) {
        try {
            List<OpenRouterMessage> openRouterMessages = messages.stream()
                    .map(message -> {
                        String role = message.getSender() == MessageSender.USER
                                ? "user"
                                : "assistant";
                        return new OpenRouterMessage(role, message.getMessage());
                    })
                    .toList();

            OpenRouterRequest request = new OpenRouterRequest(model, withSystemPrompt(productCatalog, openRouterMessages), true);

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

    @Override
    public AISearchPreferences extractSearchPreferences(List<AIMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return AISearchPreferences.empty();
        }

        String extractionPrompt = """
                You extract perfume shopping preferences from a customer chat for a
                perfume shop. The chat is a list of messages prefixed U: (customer)
                and A: (assistant). Return ONLY a single JSON object and nothing else,
                no markdown fences. Use exactly these optional keys:
                "search" (a short free-text keyword), "brand", "gender" (one of
                MEN, WOMEN, UNISEX), "fragranceFamily", "minPrice" (number),
                "maxPrice" (number). Omit any key you cannot confidently infer.
                If no preference can be inferred at all, return {"search":null}.
                """;

        try {
            List<OpenRouterMessage> openRouterMessages = messages.stream()
                    .map(message -> {
                        String role = message.getSender() == MessageSender.USER
                                ? "user"
                                : "assistant";
                        String prefix = message.getSender() == MessageSender.USER
                                ? "U: "
                                : "A: ";
                        return new OpenRouterMessage(role, prefix + message.getMessage());
                    })
                    .toList();

            List<OpenRouterMessage> payload = new ArrayList<>(openRouterMessages.size() + 1);
            payload.add(new OpenRouterMessage("system", extractionPrompt));
            payload.addAll(openRouterMessages);

            OpenRouterRequest request = new OpenRouterRequest(model, payload);

            OpenRouterResponse response = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(OpenRouterResponse.class);

            if (response == null
                    || response.getChoices() == null
                    || response.getChoices().isEmpty()
                    || response.getChoices().get(0).getMessage() == null
                    || response.getChoices().get(0).getMessage().getContent() == null) {
                return AISearchPreferences.empty();
            }

            return parseSearchPreferences(response.getChoices().get(0).getMessage().getContent());
        } catch (Exception ex) {
            log.warn("Failed to extract AI search preferences", ex);
            return AISearchPreferences.empty();
        }
    }

    private AISearchPreferences parseSearchPreferences(String rawJson) {
        try {
            String json = rawJson.trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start < 0 || end <= start) {
                return AISearchPreferences.empty();
            }
            json = json.substring(start, end + 1);
            JsonNode node = objectMapper.readTree(json);

            Gender gender = null;
            JsonNode genderNode = node.get("gender");
            if (genderNode != null && genderNode.isTextual()) {
                for (Gender candidate : Gender.values()) {
                    if (candidate.name().equalsIgnoreCase(genderNode.asText().trim())) {
                        gender = candidate;
                        break;
                    }
                }
            }

            return new AISearchPreferences(
                    textOrNull(node.get("search")),
                    textOrNull(node.get("brand")),
                    gender,
                    textOrNull(node.get("fragranceFamily")),
                    numberOrNull(node.get("minPrice")),
                    numberOrNull(node.get("maxPrice")));
        } catch (Exception ex) {
            log.warn("Failed to parse AI search preferences JSON: {}", rawJson);
            return AISearchPreferences.empty();
        }
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || !node.isValueNode()) {
            return null;
        }
        String value = node.asText(null);
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private static java.math.BigDecimal numberOrNull(JsonNode node) {
        if (node == null || !node.isNumber()) {
            return null;
        }
        return node.decimalValue();
    }
}