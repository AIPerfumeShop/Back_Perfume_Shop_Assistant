package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.ai.ScentConciergeRequest;
import com.example.spring_boot_project_api.dto.response.ai.ScentConciergeResponse;

public interface ScentConciergeService {
    ScentConciergeResponse recommend(Long userId, ScentConciergeRequest request);
}