package com.example.spring_boot_project_api.dto.request.analytics;

import com.example.spring_boot_project_api.enums.AnalyticsEventType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalyticsEventRequest {
    @NotNull
    private AnalyticsEventType eventType;

    @NotBlank
    @Size(max = 100)
    private String visitorId;

    @Size(max = 100)
    private String sessionId;

    private Long productId;

    @Size(max = 2000)
    private String metadata;
}