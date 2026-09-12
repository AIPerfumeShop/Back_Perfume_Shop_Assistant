package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.dto.request.product.ProductFilterRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.product.ProductResponse;
import com.example.spring_boot_project_api.enums.Gender;
import com.example.spring_boot_project_api.service.ProductService;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private ProductResponse productResponse() {
        ProductResponse response = new ProductResponse();
        response.setId(1L);
        response.setName("Idole");
        response.setBrand("Lancome");
        response.setDescription("A floral perfume");
        response.setCategoryId(1L);
        response.setCategoryName("Floral");
        response.setGender(Gender.WOMEN);
        response.setFragranceFamily("Floral");
        response.setAverageRate(4.5);
        response.setReviewCount(12);
        response.setInStock(true);
        response.setActive(true);
        return response;
    }

    // ---------- getAllProducts ----------

    @Test
    void getAllProducts_returnsPaged() throws Exception {
        when(productService.getAllProducts(any(ProductFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(productResponse()), 1, 1, 0, 20));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Idole"))
                .andExpect(jsonPath("$.data[0].brand").value("Lancome"))
                .andExpect(jsonPath("$.data[0].averageRate").value(4.5))
                .andExpect(jsonPath("$.data[0].reviewCount").value(12))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAllProducts_emptyResult() throws Exception {
        when(productService.getAllProducts(any(ProductFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(), 0, 0, 0, 20));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getAllProducts_withSearchFilter() throws Exception {
        when(productService.getAllProducts(any(ProductFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(productResponse()), 1, 1, 0, 20));

        mockMvc.perform(get("/api/products").param("search", "Idole"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Idole"));
    }

    @Test
    void getAllProducts_withBrandFilter() throws Exception {
        when(productService.getAllProducts(any(ProductFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(productResponse()), 1, 1, 0, 20));

        mockMvc.perform(get("/api/products").param("brand", "Lancome"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].brand").value("Lancome"));
    }

    @Test
    void getAllProducts_withCategoryFilter() throws Exception {
        when(productService.getAllProducts(any(ProductFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(productResponse()), 1, 1, 0, 20));

        mockMvc.perform(get("/api/products").param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].categoryId").value(1));
    }

    @Test
    void getAllProducts_withGenderFilter() throws Exception {
        when(productService.getAllProducts(any(ProductFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(productResponse()), 1, 1, 0, 20));

        mockMvc.perform(get("/api/products")                        .param("gender", "WOMEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].gender").value("WOMEN"));
    }

    @Test
    void getAllProducts_withPriceRange() throws Exception {
        when(productService.getAllProducts(any(ProductFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(productResponse()), 1, 1, 0, 20));

        mockMvc.perform(get("/api/products")
                        .param("minPrice", "50")
                        .param("maxPrice", "200"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllProducts_withPagination() throws Exception {
        when(productService.getAllProducts(any(ProductFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(), 0, 0, 1, 10));

        mockMvc.perform(get("/api/products")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.pageSize").value(10));
    }

    @Test
    void getAllProducts_withSort() throws Exception {
        when(productService.getAllProducts(any(ProductFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(productResponse()), 1, 1, 0, 20));

        mockMvc.perform(get("/api/products")
                        .param("sort", "name")
                        .param("direction", "desc"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllProducts_withMultipleFilters() throws Exception {
        when(productService.getAllProducts(any(ProductFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(productResponse()), 1, 1, 0, 20));

        mockMvc.perform(get("/api/products")
                        .param("search", "Idole")
                        .param("brand", "Lancome")
                        .param("gender", "WOMEN")
                        .param("inStock", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Idole"));
    }
}
