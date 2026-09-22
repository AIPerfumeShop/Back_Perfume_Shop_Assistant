package com.example.spring_boot_project_api.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.response.settings.SettingsResponse;
import com.example.spring_boot_project_api.service.SettingsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/site-content")
public class SiteContentController {
    private final SettingsService settingsService;

    public SiteContentController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    //Get public website content (settings keys prefixed with "content.")
    @Operation(summary = "Get public website content")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Website content retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<Map<String, String>> getSiteContent() {
        List<SettingsResponse> settings = settingsService.getAllSettings();
        Map<String, String> content = new LinkedHashMap<>();
        for (SettingsResponse setting : settings) {
            String key = setting.getSettingKey();
            if (key != null && key.startsWith("content.")) {
                content.put(key, setting.getValue());
            }
        }
        return ResponseEntity.ok(content);
    }
}