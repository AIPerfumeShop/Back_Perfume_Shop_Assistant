package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.product.ProductFilterRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.product.ProductResponse;
import com.example.spring_boot_project_api.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "Search products with filters, sorting and pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<PagedResponse<ProductResponse>> getAllProducts(
            @ModelAttribute ProductFilterRequest filter) {
        return ResponseEntity.ok(productService.getAllProducts(filter));
    }
}