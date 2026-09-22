package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.gift.GiftFinderRequest;
import com.example.spring_boot_project_api.dto.response.gift.GiftFinderResponse;
import com.example.spring_boot_project_api.service.GiftFinderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/gift-finder")
public class GiftFinderController {

    private final GiftFinderService giftFinderService;

    public GiftFinderController(GiftFinderService giftFinderService) {
        this.giftFinderService = giftFinderService;
    }

    @Operation(summary = "Get personalized perfume gift recommendations")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Gift recommendations returned successfully")
    })
    @PostMapping("/recommendations")
    public ResponseEntity<GiftFinderResponse> recommend(
            @Valid @RequestBody GiftFinderRequest request) {
        return ResponseEntity.ok(giftFinderService.recommend(request));
    }
}