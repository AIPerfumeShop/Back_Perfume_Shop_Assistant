package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.inventory.InventoryItemResponse;
import com.example.spring_boot_project_api.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/admin/inventory")
public class AdminInventoryController {

    private final ProductService productService;

    public AdminInventoryController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "Get inventory with optional low-stock filter and search (admin)")
    @GetMapping
    public ResponseEntity<PagedResponse<InventoryItemResponse>> getInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean lowStockOnly,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(
                productService.getInventory(page, size, lowStockOnly, search));
    }
}