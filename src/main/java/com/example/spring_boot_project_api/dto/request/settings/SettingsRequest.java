package com.example.spring_boot_project_api.dto.request.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingsRequest {
    @NotBlank(message = "Setting key is required")
    @Size(max = 100, message = "Setting key must be under 100 characters")
    private String settingKey;

    private String value;

    private String description;
}