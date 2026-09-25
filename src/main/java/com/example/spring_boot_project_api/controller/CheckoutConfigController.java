package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.response.settings.CheckoutConfigResponse;
import com.example.spring_boot_project_api.service.CheckoutConfigService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/checkout/config")
public class CheckoutConfigController {
    private final CheckoutConfigService checkoutConfigService;

    public CheckoutConfigController(CheckoutConfigService checkoutConfigService) {
        this.checkoutConfigService = checkoutConfigService;
    }

    //Public configuration the storefront needs to render checkout:
    //whether KHQR (Bakong) is enabled and the payment QR expiry window.
    @Operation(summary = "Get public checkout configuration")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Checkout configuration retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<CheckoutConfigResponse> getConfig() {
        return ResponseEntity.ok(checkoutConfigService.getConfig());
    }
}