package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.category.CreateCategoryRequest;
import com.example.spring_boot_project_api.dto.request.category.UpdateCategoryRequest;
import com.example.spring_boot_project_api.dto.response.category.CategoryResponse;

public interface CategoryService {
    //Create a new Category
    CategoryResponse createCategory(CreateCategoryRequest request);

    //Get all categories
    List<CategoryResponse> getAllCategories();

    //Get category by id
    CategoryResponse getCategoryById(Long id);
    //Update an existing category
    CategoryResponse updateCategory(Long id, UpdateCategoryRequest request);
    //Soft Delete a category
    void deactivateCategory(Long id);
}