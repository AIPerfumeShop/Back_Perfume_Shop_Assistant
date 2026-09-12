package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.settings.SettingsRequest;
import com.example.spring_boot_project_api.dto.response.settings.SettingsResponse;
import com.example.spring_boot_project_api.exception.ConflictException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.SettingsMapper;
import com.example.spring_boot_project_api.model.Settings;
import com.example.spring_boot_project_api.repository.SettingsRepository;
import com.example.spring_boot_project_api.service.SettingsService;

@Service
@Transactional
public class SettingsServiceImpl implements SettingsService {
    private final SettingsRepository settingsRepository;
    private final SettingsMapper settingsMapper;

    public SettingsServiceImpl(
            SettingsRepository settingsRepository,
            SettingsMapper settingsMapper) {
        this.settingsRepository = settingsRepository;
        this.settingsMapper = settingsMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettingsResponse> getAllSettings() {
        return settingsRepository.findAll()
                .stream()
                .map(settingsMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SettingsResponse getSettingsById(Long id) {
        Settings settings = settingsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found with ID : " + id));
        return settingsMapper.toResponse(settings);
    }

    @Override
    @Transactional(readOnly = true)
    public SettingsResponse getSettingsByKey(String key) {
        Settings settings = settingsRepository.findBySettingKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found with key : " + key));
        return settingsMapper.toResponse(settings);
    }

    @Override
    public SettingsResponse createSetting(SettingsRequest request) {
        if (settingsRepository.existsBySettingKey(request.getSettingKey().trim())) {
            throw new ConflictException(
                    "Setting key already exists : " + request.getSettingKey());
        }
        Settings settings = settingsMapper.toEntity(request);
        Settings saved = settingsRepository.save(settings);
        return settingsMapper.toResponse(saved);
    }

    @Override
    public SettingsResponse updateSetting(Long id, SettingsRequest request) {
        Settings settings = settingsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found with ID : " + id));
        if (request.getSettingKey() != null
                && !request.getSettingKey().isBlank()
                && settingsRepository.existsBySettingKeyAndIdNot(
                        request.getSettingKey().trim(), id)) {
            throw new ConflictException(
                    "Setting key already exists : " + request.getSettingKey());
        }
        settingsMapper.updateEntity(request, settings);
        Settings updated = settingsRepository.save(settings);
        return settingsMapper.toResponse(updated);
    }

    @Override
    public void deleteSetting(Long id) {
        Settings settings = settingsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found with ID : " + id));
        settingsRepository.delete(settings);
    }
}