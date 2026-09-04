package com.example.spring_boot_project_api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.brand.BrandFilterRequest;
import com.example.spring_boot_project_api.dto.request.brand.BrandRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.brand.BrandResponse;
import com.example.spring_boot_project_api.dto.response.brand.BrandStatisticsResponse;
import com.example.spring_boot_project_api.service.BrandService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/brands")
@Validated
@RequiredArgsConstructor
public class BrandController {
    private final BrandService brandService;

    @Operation(summary = "Create a new brand")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Brand created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or duplicate brand name")
    })
    @PostMapping
    public ResponseEntity<BrandResponse> createBrand(
            @Valid @RequestBody BrandRequest request) {
        BrandResponse response = brandService.createBrand(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Get all brands with optional search, active-status filter and pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Brands retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<PagedResponse<BrandResponse>> getAllBrands(
            @ModelAttribute BrandFilterRequest filter) {
        PagedResponse<BrandResponse> response = brandService.getAllBrands(filter);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get a brand by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Brand retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Brand not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BrandResponse> getBrandById(
            @PathVariable Long id) {
        BrandResponse response = brandService.getBrandById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get brand product statistics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Brand not found")
    })
    @GetMapping("/{id}/statistics")
    public ResponseEntity<BrandStatisticsResponse> getBrandStatistics(
            @PathVariable Long id) {
        BrandStatisticsResponse response = brandService.getBrandStatistics(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update an existing brand")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Brand updated successfully"),
            @ApiResponse(responseCode = "404", description = "Brand not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input or duplicate brand name")
    })
    @PutMapping("/{id}")
    public ResponseEntity<BrandResponse> updateBrand(
            @PathVariable Long id,
            @Valid @RequestBody BrandRequest request) {
        BrandResponse response = brandService.updateBrand(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Activate a brand")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Brand activated successfully"),
            @ApiResponse(responseCode = "404", description = "Brand not found"),
            @ApiResponse(responseCode = "400", description = "Brand is already active")
    })
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateBrand(@PathVariable Long id) {
        brandService.activateBrand(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Deactivate a brand")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Brand deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Brand not found"),
            @ApiResponse(responseCode = "400", description = "Brand is already deactivated")
    })
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateBrand(@PathVariable Long id) {
        brandService.deactivateBrand(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Permanently delete a brand (only when no products reference it)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Brand deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Brand not found"),
            @ApiResponse(responseCode = "409", description = "Brand still has products")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBrandPermanently(@PathVariable Long id) {
        brandService.deleteBrandPermanently(id);
        return ResponseEntity.noContent().build();
    }
}
