package com.example.spring_boot_project_api.dto.external.bakong;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BakongTokenResponse(@JsonAlias("access_token") String accessToken,
                                  @JsonAlias("token") String token,
                                  @JsonProperty("data") Data data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(@JsonAlias("access_token") String accessToken,
                       @JsonAlias("token") String token) {

        public String resolveToken() {
            return accessToken != null && !accessToken.isBlank()
                    ? accessToken
                    : token;
        }
    }

    public String resolveToken() {
        String topLevel = accessToken != null && !accessToken.isBlank()
                ? accessToken
                : token;
        if (topLevel != null && !topLevel.isBlank()) {
            return topLevel;
        }
        return data == null ? null : data.resolveToken();
    }
}