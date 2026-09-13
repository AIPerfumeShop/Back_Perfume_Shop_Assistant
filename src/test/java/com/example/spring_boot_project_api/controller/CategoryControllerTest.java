package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.dto.request.category.CreateCategoryRequest;
import com.example.spring_boot_project_api.dto.request.category.UpdateCategoryRequest;
import com.example.spring_boot_project_api.dto.response.category.CategoryResponse;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.config.JwtTokenProvider;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.CategoryService;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private CategoryResponse categoryResponse() {
        CategoryResponse response = new CategoryResponse();
        response.setId(1L);
        response.setName("Floral");
        response.setDescription("Floral fragrances");
        response.setImageUrl("https://example.com/floral.png");
        response.setIsActive(true);
        response.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        response.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        return response;
    }

    // ---------- createCategory ----------

    @Test
    void createCategory_returns201() throws Exception {
        when(categoryService.createCategory(any(CreateCategoryRequest.class)))
                .thenReturn(categoryResponse());

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Floral\", \"description\": \"Floral fragrances\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Floral"))
                .andExpect(jsonPath("$.isActive").value(true));

        verify(categoryService).createCategory(any(CreateCategoryRequest.class));
    }

    @Test
    void createCategory_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"No name\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCategory_emptyName_returns400() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCategory_duplicateName_returns400() throws Exception {
        when(categoryService.createCategory(any(CreateCategoryRequest.class)))
                .thenThrow(new BadRequestException("Category name already exists : Floral"));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Floral\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- getAllCategories ----------

    @Test
    void getAllCategories_returnsList() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of(categoryResponse()));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Floral"))
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    void getAllCategories_emptyList() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of());

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getAllCategories_multipleItems() throws Exception {
        CategoryResponse floral = categoryResponse();
        CategoryResponse woody = categoryResponse();
        woody.setId(2L);
        woody.setName("Woody");
        when(categoryService.getAllCategories()).thenReturn(List.of(floral, woody));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Floral"))
                .andExpect(jsonPath("$[1].name").value("Woody"));
    }

    // ---------- getCategoryById ----------

    @Test
    void getCategoryById_returns200() throws Exception {
        when(categoryService.getCategoryById(1L)).thenReturn(categoryResponse());

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Floral"))
                .andExpect(jsonPath("$.description").value("Floral fragrances"));
    }

    @Test
    void getCategoryById_notFound_returns404() throws Exception {
        when(categoryService.getCategoryById(404L))
                .thenThrow(new ResourceNotFoundException("Category not found with ID : 404"));

        mockMvc.perform(get("/api/categories/404"))
                .andExpect(status().isNotFound());
    }

    // ---------- updateCategory ----------

    @Test
    void updateCategory_returns200() throws Exception {
        CategoryResponse updated = categoryResponse();
        updated.setName("Fresh Floral");
        when(categoryService.updateCategory(org.mockito.ArgumentMatchers.eq(1L), any(UpdateCategoryRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Fresh Floral\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fresh Floral"));

        verify(categoryService).updateCategory(
                org.mockito.ArgumentMatchers.eq(1L), any(UpdateCategoryRequest.class));
    }

    @Test
    void updateCategory_notFound_returns404() throws Exception {
        when(categoryService.updateCategory(org.mockito.ArgumentMatchers.eq(404L), any(UpdateCategoryRequest.class)))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(put("/api/categories/404")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"New\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCategory_duplicateName_returns400() throws Exception {
        when(categoryService.updateCategory(org.mockito.ArgumentMatchers.eq(1L), any(UpdateCategoryRequest.class)))
                .thenThrow(new BadRequestException("Category name already exists"));

        mockMvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Duplicate\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCategory_missingName_returns400() throws Exception {
        mockMvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- deactivateCategory ----------

    @Test
    void deactivateCategory_returns204() throws Exception {
        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isNoContent());

        verify(categoryService).deactivateCategory(1L);
    }

    @Test
    void deactivateCategory_alreadyInactive_returns400() throws Exception {
        doThrow(new BadRequestException("Category is already deactivated"))
                .when(categoryService).deactivateCategory(1L);

        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deactivateCategory_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Category not found"))
                .when(categoryService).deactivateCategory(404L);

        mockMvc.perform(delete("/api/categories/404"))
                .andExpect(status().isNotFound());
    }
}
