package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.settings.SettingsRequest;
import com.example.spring_boot_project_api.dto.response.settings.SettingsResponse;
import com.example.spring_boot_project_api.service.SettingsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/settings")
@Validated
public class SettingsController {
    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    //Get all settings
    @Operation(summary = "Get all settings")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Settings retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<SettingsResponse>> getAllSettings() {
        return ResponseEntity.ok(settingsService.getAllSettings());
    }

    //Get setting by ID
    @Operation(summary = "Get a setting by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Setting retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Setting not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<SettingsResponse> getSettingById(@PathVariable Long id) {
        return ResponseEntity.ok(settingsService.getSettingsById(id));
    }

    //Get setting by key
    @Operation(summary = "Get a setting by key")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Setting retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Setting not found")
    })
    @GetMapping("/key/{key}")
    public ResponseEntity<SettingsResponse> getSettingByKey(@PathVariable String key) {
        return ResponseEntity.ok(settingsService.getSettingsByKey(key));
    }

    //Create setting
    @Operation(summary = "Create a new setting")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Setting created successfully"),
        @ApiResponse(responseCode = "409", description = "Setting key already exists")
    })
    @PostMapping
    public ResponseEntity<SettingsResponse> createSetting(
            @Valid @RequestBody SettingsRequest request) {
        SettingsResponse response = settingsService.createSetting(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //Update setting
    @Operation(summary = "Update a setting")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Setting updated successfully"),
        @ApiResponse(responseCode = "404", description = "Setting not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<SettingsResponse> updateSetting(
            @PathVariable Long id,
            @Valid @RequestBody SettingsRequest request) {
        return ResponseEntity.ok(settingsService.updateSetting(id, request));
    }

    //Delete setting
    @Operation(summary = "Delete a setting")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Setting deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Setting not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSetting(@PathVariable Long id) {
        settingsService.deleteSetting(id);
        return ResponseEntity.noContent().build();
    }
}