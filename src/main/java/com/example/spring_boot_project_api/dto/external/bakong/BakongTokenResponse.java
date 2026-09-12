package com.example.spring_boot_project_api.dto.external.bakong;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BakongTokenResponse(@JsonAlias("access_token") String accessToken,
                                  @JsonAlias("token") String token) {

    public String resolveToken() {
        return accessToken != null && !accessToken.isBlank()
                ? accessToken
                : token;
    }
}