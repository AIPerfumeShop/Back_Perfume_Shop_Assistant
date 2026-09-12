package com.example.spring_boot_project_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.spring_boot_project_api.dto.request.category.CreateCategoryRequest;
import com.example.spring_boot_project_api.dto.request.category.UpdateCategoryRequest;
import com.example.spring_boot_project_api.dto.response.category.CategoryResponse;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.CategoryMapper;
import com.example.spring_boot_project_api.model.Category;
import com.example.spring_boot_project_api.repository.CategoryRepository;
import com.example.spring_boot_project_api.service.impl.CategoryServiceImpl;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryRepository, new CategoryMapper());
    }

    private Category category(Long id, String name, boolean active) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setDescription("Description for " + name);
        category.setImageUrl("https://example.com/" + name.toLowerCase() + ".png");
        category.setIsActive(active);
        category.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        category.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        return category;
    }

    // ---------- createCategory ----------

    @Test
    void createCategory_success() {
        when(categoryRepository.existsByNameIgnoreCase("Floral")).thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(inv -> {
                    Category c = inv.getArgument(0);
                    c.setId(1L);
                    return c;
                });

        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Floral");
        request.setDescription("Floral fragrances");

        CategoryResponse response = categoryService.createCategory(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Floral", response.getName());
        assertEquals("Floral fragrances", response.getDescription());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_duplicateName_throwsBadRequest() {
        when(categoryRepository.existsByNameIgnoreCase("Floral")).thenReturn(true);

        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Floral");

        assertThrows(BadRequestException.class,
                () -> categoryService.createCategory(request));

        verify(categoryRepository, never()).save(any(Category.class));
    }

    // ---------- getAllCategories ----------

    @Test
    void getAllCategories_returnsActiveOnly() {
        Category active = category(1L, "Floral", true);
        Category inactive = category(2L, "Woody", false);
        when(categoryRepository.findAll()).thenReturn(List.of(active, inactive));

        List<CategoryResponse> responses = categoryService.getAllCategories();

        assertEquals(1, responses.size());
        assertEquals("Floral", responses.get(0).getName());
    }

    @Test
    void getAllCategories_emptyList() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        List<CategoryResponse> responses = categoryService.getAllCategories();

        assertTrue(responses.isEmpty());
    }

    @Test
    void getAllCategories_allActive() {
        Category floral = category(1L, "Floral", true);
        Category woody = category(2L, "Woody", true);
        when(categoryRepository.findAll()).thenReturn(List.of(floral, woody));

        List<CategoryResponse> responses = categoryService.getAllCategories();

        assertEquals(2, responses.size());
    }

    // ---------- getCategoryById ----------

    @Test
    void getCategoryById_success() {
        Category category = category(1L, "Floral", true);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.getCategoryById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Floral", response.getName());
    }

    @Test
    void getCategoryById_notFound_throws() {
        when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.getCategoryById(404L));
    }

    @Test
    void getCategoryById_inactive_throws() {
        Category category = category(1L, "Floral", false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.getCategoryById(1L));
    }

    // ---------- updateCategory ----------

    @Test
    void updateCategory_success() {
        Category category = category(1L, "Floral", true);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Fresh Floral", 1L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("Fresh Floral");
        request.setDescription("Updated description");

        CategoryResponse response = categoryService.updateCategory(1L, request);

        assertEquals("Fresh Floral", response.getName());
        verify(categoryRepository).save(category);
    }

    @Test
    void updateCategory_notFound_throws() {
        when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("New");

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.updateCategory(404L, request));
    }

    @Test
    void updateCategory_inactive_throws() {
        Category category = category(1L, "Floral", false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("New");

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.updateCategory(1L, request));
    }

    @Test
    void updateCategory_duplicateName_throws() {
        Category category = category(1L, "Floral", true);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Duplicate", 1L)).thenReturn(true);

        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setName("Duplicate");

        assertThrows(BadRequestException.class,
                () -> categoryService.updateCategory(1L, request));
    }

    // ---------- deactivateCategory ----------

    @Test
    void deactivateCategory_success() {
        Category category = category(1L, "Floral", true);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        categoryService.deactivateCategory(1L);

        assertFalse(category.getIsActive());
        verify(categoryRepository).save(category);
    }

    @Test
    void deactivateCategory_alreadyInactive_throws() {
        Category category = category(1L, "Floral", false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThrows(BadRequestException.class,
                () -> categoryService.deactivateCategory(1L));
    }

    @Test
    void deactivateCategory_notFound_throws() {
        when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.deactivateCategory(404L));
    }
}
