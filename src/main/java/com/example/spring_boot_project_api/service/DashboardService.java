package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.response.dashboard.DashboardSummaryResponse;

public interface DashboardService {
    DashboardSummaryResponse getSummary();
}