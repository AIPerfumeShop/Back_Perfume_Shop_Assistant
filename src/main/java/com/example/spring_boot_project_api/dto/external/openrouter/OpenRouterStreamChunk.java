package com.example.spring_boot_project_api.dto.external.openrouter;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenRouterStreamChunk {
    private List<OpenRouterStreamChoice> choices;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenRouterStreamChoice {
        private Delta delta;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Delta {
        private String content;
    }
}