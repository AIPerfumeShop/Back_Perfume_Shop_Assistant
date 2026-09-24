package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.analytics.AnalyticsFilterRequest;
import com.example.spring_boot_project_api.dto.response.ai.AdminAnomalyDetectionResponse;
import com.example.spring_boot_project_api.dto.response.ai.AdminBusinessBriefingResponse;
import com.example.spring_boot_project_api.dto.response.ai.AdminCustomerIntelligenceResponse;
import com.example.spring_boot_project_api.dto.response.ai.AdminCustomerPainPointsResponse;
import com.example.spring_boot_project_api.dto.response.ai.AdminConversionIntelligenceResponse;
import com.example.spring_boot_project_api.dto.response.ai.AdminInventoryForecastResponse;
import com.example.spring_boot_project_api.dto.response.ai.AdminSpendingIntelligenceResponse;

public interface AdminAIAnalyticsService {
    AdminSpendingIntelligenceResponse getSpendingIntelligence(AnalyticsFilterRequest filter);

    AdminInventoryForecastResponse getInventoryForecast(AnalyticsFilterRequest filter);

    AdminAnomalyDetectionResponse getAnomalyDetection(AnalyticsFilterRequest filter);

    AdminCustomerPainPointsResponse getCustomerPainPoints(AnalyticsFilterRequest filter);

    AdminCustomerIntelligenceResponse getCustomerIntelligence(AnalyticsFilterRequest filter);

    AdminConversionIntelligenceResponse getConversionIntelligence(AnalyticsFilterRequest filter);

    AdminBusinessBriefingResponse getBusinessBriefing(AnalyticsFilterRequest filter);
}