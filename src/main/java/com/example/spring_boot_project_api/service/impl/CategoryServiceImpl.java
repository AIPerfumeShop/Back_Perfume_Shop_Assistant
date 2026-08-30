package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.category.CreateCategoryRequest;
import com.example.spring_boot_project_api.dto.request.category.UpdateCategoryRequest;
import com.example.spring_boot_project_api.dto.response.category.CategoryResponse;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.CategoryMapper;
import com.example.spring_boot_project_api.model.Category;
import com.example.spring_boot_project_api.repository.CategoryRepository;
import com.example.spring_boot_project_api.service.CategoryService;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(
            CategoryRepository categoryRepository,
            CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    // Create Category
    @Override
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        // Check duplicate category name
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException(
                    "Category name already exists : " + request.getName());
        }
        // Convert request DTO -> Entity
        Category category = categoryMapper.toEntity(request);

        // Save category
        Category saveCategory = categoryRepository.save(category);

        // Convert Entity -> Response DTO
        return categoryMapper.toResponse(saveCategory);
    }

    // Get all categories
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .filter(category -> Boolean.TRUE.equals(category.getIsActive()))
                .map(categoryMapper::toResponse)
                .toList();

    }

    //Get Category By Id
    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found with ID : " + id)
                );
        //Do not return soft-deleted categories
        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new ResourceNotFoundException("Category not found with ID : " + id);
        }
        return categoryMapper.toResponse(category);
    }

    //Update Category
    @Override
    public CategoryResponse updateCategory(
            Long id, UpdateCategoryRequest request) {
        //find existing category
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found with ID : " + id));
        //Do not update a deactivated Category
        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new ResourceNotFoundException("Category not found with ID : " + id);
        }
        //Check duplicate category name (excluding self)
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new BadRequestException(
                    "Category name already exists : " + request.getName());
        }
        //Update existing Entity
        categoryMapper.updateEntity(request, category);
        //save update category
        Category updateCategory = categoryRepository.save(category);
        //return response
        return categoryMapper.toResponse(updateCategory);
    }

    //Soft Delete
    @Override
    public void deactivateCategory(Long id) {
        //Find existing category
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found with ID : " + id));
        //Check if already deactivated
        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new BadRequestException("Category is already deactivated");
        }
        //Soft Delete
        category.setIsActive(false);

        //Save Change
        categoryRepository.save(category);
    }
}