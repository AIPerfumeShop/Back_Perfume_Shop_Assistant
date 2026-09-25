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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
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

    private static final List<String> FREE_GEMINI_MODELS = List.of(
            "gemini-3.8-flash",
            "gemini-3.5-flash-lite",
            "gemini-3.5-flash",
            "gemini-3.1-flash-lite");

    private static final Logger log = LoggerFactory.getLogger(OpenRouterServiceImpl.class);

    private final RestClient restClient;

    private final RestClient geminiClient;

    private final String apiKey;

    private final String model;

    private final String geminiApiKey;

    private final String geminiModel;

    private final String systemPrompt;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenRouterServiceImpl(
            @Value("${openrouter.url}") String url,
            @Value("${openrouter.api-key}") String apiKey,
            @Value("${openrouter.model}") String model,
            @Value("${gemini.api-key:}") String geminiApiKey,
            @Value("${gemini.model:gemini-3.8-flash}") String geminiModel,
            @Value("${ai.system-prompt:"
                    + "You are the friendly assistant of Blossom Fragrance perfume shop. "
                    + "Recommend only perfumes that exist in the shop catalog. "
                    + "Keep answers SHORT and easy to skim: at most 3-6 short bullet points "
                    + "or 2-4 sentences. Never write long essays, huge tables, or long "
                    + "introductions. Skip closing questions like 'what would you like next?' "
                    + "unless the customer asks for help narrowing down.}") String systemPrompt) {

        this.apiKey = apiKey;
        this.model = model;
        this.geminiApiKey = geminiApiKey;
        this.geminiModel = geminiModel;
        this.systemPrompt = systemPrompt;

        this.restClient = RestClient.builder()
                .baseUrl(url)
                .build();
        this.geminiClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/openai")
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
            return generateWithGeminiOrThrow(messages, productCatalog, ex);

        } catch (RestClientResponseException ex) {
            return generateWithGeminiOrThrow(messages, productCatalog, providerResponseFailure(ex, "chat request"));

        } catch (ResourceAccessException ex) {
            log.error("OpenRouter is unreachable during non-stream call", ex);
            return generateWithGeminiOrThrow(messages, productCatalog,
                    new AIServiceException("OpenRouter could not be reached. Check the backend network connection.", ex));

        } catch (Exception ex) {

            // Convert OpenRouter errors into our AI exception
            log.error("OpenRouter non-stream call failed", ex);
            return generateWithGeminiOrThrow(messages, productCatalog,
                    new AIServiceException("Failed to communicate with OpenRouter", ex));
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
            streamWithGeminiOrThrow(messages, productCatalog, onToken, ex);
        } catch (RestClientResponseException ex) {
            streamWithGeminiOrThrow(messages, productCatalog, onToken, providerResponseFailure(ex, "stream request"));
        } catch (ResourceAccessException ex) {
            log.error("OpenRouter is unreachable during stream call", ex);
            streamWithGeminiOrThrow(messages, productCatalog, onToken,
                    new AIServiceException("OpenRouter could not be reached. Check the backend network connection.", ex));
        } catch (Exception ex) {
            log.error("OpenRouter stream call failed", ex);
            streamWithGeminiOrThrow(messages, productCatalog, onToken,
                    new AIServiceException("Failed to stream response from OpenRouter", ex));
        }
    }

    private AIServiceException providerResponseFailure(RestClientResponseException ex, String operation) {
        int status = ex.getStatusCode().value();
        String guidance = switch (status) {
            case 401, 403 -> "Check that OPENROUTER_API_KEY is valid in the backend environment.";
            case 402 -> "OpenRouter rejected the account or provider quota for this request.";
            case 429 -> "OpenRouter rate limit reached. Wait briefly and try again.";
            default -> status >= 500
                    ? "The OpenRouter provider is temporarily unavailable. Try again shortly."
                    : "Check the request configuration and backend logs.";
        };
        log.error("OpenRouter {} failed with HTTP {}", operation, status, ex);
        return new AIServiceException("OpenRouter returned HTTP " + status + ". " + guidance, ex);
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

        try {

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
            log.warn("OpenRouter preference extraction failed; trying Gemini fallback", ex);
            if (!isGeminiConfigured()) return AISearchPreferences.empty();
            for (String candidateModel : geminiModelCandidates()) {
                try {
                    OpenRouterRequest fallbackRequest = new OpenRouterRequest(candidateModel, payload);
                    OpenRouterResponse fallbackResponse = geminiClient.post()
                            .uri("/chat/completions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + geminiApiKey)
                            .body(fallbackRequest)
                            .retrieve()
                            .body(OpenRouterResponse.class);
                    if (fallbackResponse != null && fallbackResponse.getChoices() != null
                            && !fallbackResponse.getChoices().isEmpty()
                            && fallbackResponse.getChoices().get(0).getMessage() != null
                            && fallbackResponse.getChoices().get(0).getMessage().getContent() != null) {
                        return parseSearchPreferences(fallbackResponse.getChoices().get(0).getMessage().getContent());
                    }
                } catch (Exception fallbackException) {
                    log.warn("Gemini preference extraction failed for model {}", candidateModel, fallbackException);
                }
            }
            return AISearchPreferences.empty();
        }
    }

    private boolean isGeminiConfigured() {
        return geminiApiKey != null && !geminiApiKey.isBlank();
    }

    private List<String> geminiModelCandidates() {
        List<String> candidates = new ArrayList<>();
        if (FREE_GEMINI_MODELS.contains(geminiModel)) {
            candidates.add(geminiModel);
        } else if (geminiModel != null && !geminiModel.isBlank()) {
            log.warn("Configured Gemini model is not on the free-model allowlist; ignoring it");
        }
        FREE_GEMINI_MODELS.stream().filter(candidate -> !candidates.contains(candidate)).forEach(candidates::add);
        return candidates;
    }

    private String generateWithGeminiOrThrow(List<AIMessage> messages, String productCatalog, AIServiceException openRouterFailure) {
        if (!isGeminiConfigured()) throw openRouterFailure;
        log.warn("OpenRouter failed; retrying this AI request with Gemini");
        List<OpenRouterMessage> payload = messages.stream()
                .map(message -> new OpenRouterMessage(
                        message.getSender() == MessageSender.USER ? "user" : "assistant", message.getMessage()))
                .toList();
        Exception lastFailure = null;
        for (String candidateModel : geminiModelCandidates()) {
            try {
                OpenRouterResponse response = geminiClient.post()
                        .uri("/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + geminiApiKey)
                        .body(new OpenRouterRequest(candidateModel, withSystemPrompt(productCatalog, payload)))
                        .retrieve().body(OpenRouterResponse.class);
                if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()
                        && response.getChoices().get(0).getMessage() != null
                        && response.getChoices().get(0).getMessage().getContent() != null) {
                    return response.getChoices().get(0).getMessage().getContent();
                }
                lastFailure = new AIServiceException("Gemini returned no response for model " + candidateModel);
            } catch (Exception geminiFailure) {
                lastFailure = geminiFailure;
                log.warn("Gemini fallback failed for model {}; trying next configured fallback", candidateModel, geminiFailure);
            }
        }
        log.error("All Gemini fallback models failed", lastFailure);
        throw new AIServiceException("OpenRouter and Gemini both failed. Check provider keys, quotas, and backend logs.", lastFailure);
    }

    private void streamWithGeminiOrThrow(List<AIMessage> messages, String productCatalog, Consumer<String> onToken,
            AIServiceException openRouterFailure) {
        if (!isGeminiConfigured()) throw openRouterFailure;
        log.warn("OpenRouter streaming failed; retrying this AI request with Gemini");
        List<OpenRouterMessage> payload = messages.stream()
                .map(message -> new OpenRouterMessage(
                        message.getSender() == MessageSender.USER ? "user" : "assistant", message.getMessage()))
                .toList();
        Exception lastFailure = null;
        for (String candidateModel : geminiModelCandidates()) {
            java.util.concurrent.atomic.AtomicBoolean emittedToken = new java.util.concurrent.atomic.AtomicBoolean();
            try {
                OpenRouterRequest request = new OpenRouterRequest(candidateModel, withSystemPrompt(productCatalog, payload), true);
                geminiClient.post().uri("/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .header("Authorization", "Bearer " + geminiApiKey)
                        .body(request)
                        .exchange((httpRequest, httpResponse) -> {
                            if (httpResponse.getStatusCode().isError()) {
                                throw new IllegalStateException("Gemini returned HTTP " + httpResponse.getStatusCode().value());
                            }
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getBody(), StandardCharsets.UTF_8))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if (!line.startsWith(DATA_PREFIX)) continue;
                                    String data = line.substring(DATA_PREFIX.length()).trim();
                                    if (data.isEmpty() || DONE.equals(data)) continue;
                                    OpenRouterStreamChunk chunk = objectMapper.readValue(data, OpenRouterStreamChunk.class);
                                    if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()
                                            && chunk.getChoices().get(0).getDelta() != null) {
                                        String content = chunk.getChoices().get(0).getDelta().getContent();
                                        if (content != null && !content.isEmpty()) {
                                            emittedToken.set(true);
                                            onToken.accept(content);
                                        }
                                    }
                                }
                            }
                            return null;
                        });
                if (emittedToken.get()) return;
                lastFailure = new IllegalStateException("Gemini returned no streamed content for model " + candidateModel);
            } catch (Exception geminiFailure) {
                if (emittedToken.get()) {
                    throw new AIServiceException("Gemini stream was interrupted after starting the response.", geminiFailure);
                }
                lastFailure = geminiFailure;
                log.warn("Gemini streaming failed for model {}; trying next fallback", candidateModel, geminiFailure);
            }
        }
        log.error("All Gemini streaming fallback models failed", lastFailure);
        throw new AIServiceException("OpenRouter and Gemini both failed. Check provider keys, quotas, and backend logs.", lastFailure);
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
