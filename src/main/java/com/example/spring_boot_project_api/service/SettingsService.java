package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.settings.SettingsRequest;
import com.example.spring_boot_project_api.dto.response.settings.SettingsResponse;

public interface SettingsService {
    List<SettingsResponse> getAllSettings();

    SettingsResponse getSettingsById(Long id);

    SettingsResponse getSettingsByKey(String key);

    SettingsResponse createSetting(SettingsRequest request);

    SettingsResponse updateSetting(Long id, SettingsRequest request);

    void deleteSetting(Long id);
}