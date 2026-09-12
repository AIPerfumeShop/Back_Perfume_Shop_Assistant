package com.example.spring_boot_project_api.service.impl;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.example.spring_boot_project_api.config.BakongProperties;
import com.example.spring_boot_project_api.dto.external.bakong.BakongTokenRequest;
import com.example.spring_boot_project_api.dto.external.bakong.BakongTokenResponse;
import com.example.spring_boot_project_api.exception.BakongException;
import com.example.spring_boot_project_api.service.BakongTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class BakongTokenServiceImpl implements BakongTokenService {

    private static final Logger log = LoggerFactory.getLogger(BakongTokenServiceImpl.class);

    private static final String RENEW_TOKEN_PATH = "/v1/renew_token";
    private static final long SAFETY_BUFFER_SECONDS = 300;

    private final BakongProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String cachedToken;
    private long cachedTokenExpiresAtEpochSeconds;

    public BakongTokenServiceImpl(BakongProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public synchronized String getAccessToken() {
        if (isTokenValid()) {
            return cachedToken;
        }
        String token = fetchFreshToken();
        cachedToken = token;
        cachedTokenExpiresAtEpochSeconds = extractExpiry(token);
        log.info("Bakong access token renewed, expires at {}",
                Instant.ofEpochSecond(cachedTokenExpiresAtEpochSeconds));
        return cachedToken;
    }

    private boolean isTokenValid() {
        return cachedToken != null
                && Instant.now().getEpochSecond() + SAFETY_BUFFER_SECONDS
                        < cachedTokenExpiresAtEpochSeconds;
    }

    private String fetchFreshToken() {
        if (!properties.isConfigured()) {
            throw new BakongException(
                    "Bakong is not configured. Set BAKONG_ACCOUNT_ID, "
                            + "BAKONG_BASE_URL and EMAIL in .env");
        }
        try {
            BakongTokenResponse response = restClient.post()
                    .uri(RENEW_TOKEN_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new BakongTokenRequest(properties.getEmail()))
                    .retrieve()
                    .body(BakongTokenResponse.class);

            String token = response == null ? null : response.resolveToken();
            if (token == null || token.isBlank()) {
                throw new BakongException(
                        "Bakong token response did not contain an access token");
            }
            return token;
        } catch (BakongException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BakongException("Failed to renew Bakong access token", ex);
        }
    }

    private long extractExpiry(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Token is not a JWT");
            }
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode root = objectMapper.readTree(
                    new String(payload, StandardCharsets.UTF_8));
            return root.path("exp").asLong();
        } catch (Exception ex) {
            throw new BakongException("Failed to decode Bakong token expiry", ex);
        }
    }
}