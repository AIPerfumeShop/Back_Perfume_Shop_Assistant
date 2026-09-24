package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.analytics.AnalyticsEventRequest;
import com.example.spring_boot_project_api.service.AnalyticsEventService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/analytics/events")
public class AnalyticsEventController {
    private final AnalyticsEventService analyticsEventService;

    public AnalyticsEventController(AnalyticsEventService analyticsEventService) {
        this.analyticsEventService = analyticsEventService;
    }

    @Operation(summary = "Record a storefront analytics event")
    @PostMapping
    public ResponseEntity<Void> record(@Valid @RequestBody AnalyticsEventRequest request) {
        analyticsEventService.record(request);
        return ResponseEntity.accepted().build();
    }
}