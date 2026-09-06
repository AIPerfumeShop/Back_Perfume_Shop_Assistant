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

import com.example.spring_boot_project_api.dto.request.category.CreateCategoryRequest;
import com.example.spring_boot_project_api.dto.request.category.UpdateCategoryRequest;
import com.example.spring_boot_project_api.dto.response.category.CategoryResponse;
import com.example.spring_boot_project_api.service.CategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categories")
@Validated
public class CategoryController {
    private final CategoryService categoryService;
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }
    //Create Category
    @Operation(summary = "Create a new category")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Category created successfully")
    })
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
        @Valid @RequestBody CreateCategoryRequest request
    ) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }
    //Get all categories
    @Operation(summary = "Get all categories")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> response = categoryService.getAllCategories();
        return ResponseEntity.ok(response);
    }

    //Get Category By Id
    @Operation(summary = "Get a category by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(
        @PathVariable Long id) {
            CategoryResponse response = categoryService.getCategoryById(id);

            return ResponseEntity.ok(response);
        }
    //Update Category
    @Operation(summary = "Update a category")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category updated successfully")
    })
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
        @PathVariable Long id,
        @Valid @RequestBody UpdateCategoryRequest request) {
            CategoryResponse response = categoryService.updateCategory(id, request);

            return ResponseEntity.ok(response);
        }
    //Soft Delete Category
    @Operation(summary = "Deactivate (soft delete) a category")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Category deactivated successfully")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateCategory(@PathVariable Long id) {
        categoryService.deactivateCategory(id);

        return ResponseEntity.noContent().build();
    }
}