package com.example.spring_boot_project_api.dto.external.openrouter;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpenRouterResponse {
    private List<OpenRouterChoice> choices;
}
