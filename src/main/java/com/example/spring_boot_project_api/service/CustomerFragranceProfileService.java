package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.ai.FragranceProfileRequest;
import com.example.spring_boot_project_api.dto.response.ai.CustomerFragranceProfileResponse;

public interface CustomerFragranceProfileService {
    CustomerFragranceProfileResponse getOrGenerate(Long userId);
    CustomerFragranceProfileResponse update(Long userId, FragranceProfileRequest request);
}
