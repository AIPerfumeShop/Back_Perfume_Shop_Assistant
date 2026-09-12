package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.settings.SettingsRequest;
import com.example.spring_boot_project_api.dto.response.settings.SettingsResponse;
import com.example.spring_boot_project_api.model.Settings;

@Component
public class SettingsMapper {
    // SettingsRequest -> Settings
    public Settings toEntity(SettingsRequest request) {
        if (request == null) {
            return null;
        }
        Settings settings = new Settings();
        settings.setSettingKey(request.getSettingKey().trim());
        settings.setValue(request.getValue());
        settings.setDescription(request.getDescription());
        return settings;
    }

    // Settings -> SettingsResponse
    public SettingsResponse toResponse(Settings settings) {
        if (settings == null) {
            return null;
        }
        SettingsResponse response = new SettingsResponse();
        response.setId(settings.getId());
        response.setSettingKey(settings.getSettingKey());
        response.setValue(settings.getValue());
        response.setDescription(settings.getDescription());
        response.setCreatedAt(settings.getCreatedAt());
        response.setUpdatedAt(settings.getUpdatedAt());
        return response;
    }

    // Update existing Settings from SettingsRequest
    public void updateEntity(SettingsRequest request, Settings settings) {
        if (request == null || settings == null) {
            return;
        }
        if (request.getSettingKey() != null && !request.getSettingKey().isBlank()) {
            settings.setSettingKey(request.getSettingKey().trim());
        }
        settings.setValue(request.getValue());
        settings.setDescription(request.getDescription());
    }
}