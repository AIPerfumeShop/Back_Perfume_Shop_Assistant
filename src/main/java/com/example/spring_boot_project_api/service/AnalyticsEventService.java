package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.analytics.AnalyticsEventRequest;

public interface AnalyticsEventService {
    void record(AnalyticsEventRequest request);
}