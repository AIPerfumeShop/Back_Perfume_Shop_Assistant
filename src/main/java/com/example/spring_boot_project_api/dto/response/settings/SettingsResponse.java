package com.example.spring_boot_project_api.dto.response.settings;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingsResponse {
    private Long id;
    private String settingKey;
    private String value;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}