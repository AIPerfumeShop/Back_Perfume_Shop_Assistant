package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.dto.request.brand.BrandFilterRequest;
import com.example.spring_boot_project_api.dto.request.brand.BrandRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.brand.BrandResponse;
import com.example.spring_boot_project_api.dto.response.brand.BrandStatisticsResponse;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ConflictException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.config.JwtTokenProvider;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.BrandService;
import com.example.spring_boot_project_api.service.ProductService;

@WebMvcTest(BrandController.class)
@AutoConfigureMockMvc(addFilters = false)
class BrandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrandService brandService;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private BrandResponse brandResponse() {
        BrandResponse response = new BrandResponse();
        response.setId(1L);
        response.setName("Lancome");
        response.setDescription("French luxury perfume house");
        response.setLogoUrl("https://example.com/lancome.png");
        response.setIsActive(true);
        response.setProductsCount(5L);
        response.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        response.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        return response;
    }

    private BrandStatisticsResponse statsResponse() {
        BrandStatisticsResponse stats = new BrandStatisticsResponse();
        stats.setBrandId(1L);
        stats.setBrandName("Lancome");
        stats.setTotalProducts(5L);
        stats.setActiveProducts(4L);
        stats.setInactiveProducts(1L);
        return stats;
    }

    // ---------- createBrand ----------

    @Test
    void createBrand_returns201() throws Exception {
        when(brandService.createBrand(any(BrandRequest.class))).thenReturn(brandResponse());

        mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lancome\", \"description\": \"French luxury perfume house\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Lancome"))
                .andExpect(jsonPath("$.productsCount").value(5));

        verify(brandService).createBrand(any(BrandRequest.class));
    }

    @Test
    void createBrand_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"No name\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBrand_emptyName_returns400() throws Exception {
        mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBrand_duplicateName_returns400() throws Exception {
        when(brandService.createBrand(any(BrandRequest.class)))
                .thenThrow(new BadRequestException("Brand name already exists : Lancome"));

        mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lancome\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- getAllBrands ----------

    @Test
    void getAllBrands_returnsPaged() throws Exception {
        when(brandService.getAllBrands(any(BrandFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(brandResponse()), 1, 1, 0, 20));

        mockMvc.perform(get("/api/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Lancome"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAllBrands_emptyResult() throws Exception {
        when(brandService.getAllBrands(any(BrandFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(), 0, 0, 0, 20));

        mockMvc.perform(get("/api/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getAllBrands_withSearchFilter() throws Exception {
        when(brandService.getAllBrands(any(BrandFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(brandResponse()), 1, 1, 0, 20));

        mockMvc.perform(get("/api/brands").param("search", "Lanc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Lancome"));
    }

    // ---------- getBrandById ----------

    @Test
    void getBrandById_returns200() throws Exception {
        when(brandService.getBrandById(1L)).thenReturn(brandResponse());

        mockMvc.perform(get("/api/brands/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Lancome"))
                .andExpect(jsonPath("$.productsCount").value(5));
    }

    @Test
    void getBrandById_notFound_returns404() throws Exception {
        when(brandService.getBrandById(404L))
                .thenThrow(new ResourceNotFoundException("Brand not found with ID : 404"));

        mockMvc.perform(get("/api/brands/404"))
                .andExpect(status().isNotFound());
    }

    // ---------- getBrandStatistics ----------

    @Test
    void getBrandStatistics_returns200() throws Exception {
        when(brandService.getBrandStatistics(1L)).thenReturn(statsResponse());

        mockMvc.perform(get("/api/brands/1/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brandId").value(1))
                .andExpect(jsonPath("$.brandName").value("Lancome"))
                .andExpect(jsonPath("$.totalProducts").value(5))
                .andExpect(jsonPath("$.activeProducts").value(4))
                .andExpect(jsonPath("$.inactiveProducts").value(1));
    }

    @Test
    void getBrandStatistics_notFound_returns404() throws Exception {
        when(brandService.getBrandStatistics(404L))
                .thenThrow(new ResourceNotFoundException("Brand not found"));

        mockMvc.perform(get("/api/brands/404/statistics"))
                .andExpect(status().isNotFound());
    }

    // ---------- updateBrand ----------

    @Test
    void updateBrand_returns200() throws Exception {
        BrandResponse updated = brandResponse();
        updated.setName("Lancome Updated");
        when(brandService.updateBrand(org.mockito.ArgumentMatchers.eq(1L), any(BrandRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/brands/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lancome Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lancome Updated"));

        verify(brandService).updateBrand(org.mockito.ArgumentMatchers.eq(1L), any(BrandRequest.class));
    }

    @Test
    void updateBrand_notFound_returns404() throws Exception {
        when(brandService.updateBrand(org.mockito.ArgumentMatchers.eq(404L), any(BrandRequest.class)))
                .thenThrow(new ResourceNotFoundException("Brand not found"));

        mockMvc.perform(put("/api/brands/404")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"New\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateBrand_duplicateName_returns400() throws Exception {
        when(brandService.updateBrand(org.mockito.ArgumentMatchers.eq(1L), any(BrandRequest.class)))
                .thenThrow(new BadRequestException("Brand name already exists"));

        mockMvc.perform(put("/api/brands/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Duplicate\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateBrand_missingName_returns400() throws Exception {
        mockMvc.perform(put("/api/brands/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- activateBrand ----------

    @Test
    void activateBrand_returns204() throws Exception {
        mockMvc.perform(patch("/api/brands/1/activate"))
                .andExpect(status().isNoContent());

        verify(brandService).activateBrand(1L);
    }

    @Test
    void activateBrand_alreadyActive_returns400() throws Exception {
        doThrow(new BadRequestException("Brand is already active"))
                .when(brandService).activateBrand(1L);

        mockMvc.perform(patch("/api/brands/1/activate"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void activateBrand_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Brand not found"))
                .when(brandService).activateBrand(404L);

        mockMvc.perform(patch("/api/brands/404/activate"))
                .andExpect(status().isNotFound());
    }

    // ---------- deactivateBrand ----------

    @Test
    void deactivateBrand_returns204() throws Exception {
        mockMvc.perform(patch("/api/brands/1/deactivate"))
                .andExpect(status().isNoContent());

        verify(brandService).deactivateBrand(1L);
    }

    @Test
    void deactivateBrand_alreadyInactive_returns400() throws Exception {
        doThrow(new BadRequestException("Brand is already deactivated"))
                .when(brandService).deactivateBrand(1L);

        mockMvc.perform(patch("/api/brands/1/deactivate"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deactivateBrand_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Brand not found"))
                .when(brandService).deactivateBrand(404L);

        mockMvc.perform(patch("/api/brands/404/deactivate"))
                .andExpect(status().isNotFound());
    }

    // ---------- deleteBrandPermanently ----------

    @Test
    void deleteBrand_returns204() throws Exception {
        mockMvc.perform(delete("/api/brands/1"))
                .andExpect(status().isNoContent());

        verify(brandService).deleteBrandPermanently(1L);
    }

    @Test
    void deleteBrand_hasProducts_returns409() throws Exception {
        doThrow(new ConflictException("Cannot delete brand with products"))
                .when(brandService).deleteBrandPermanently(1L);

        mockMvc.perform(delete("/api/brands/1"))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteBrand_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Brand not found"))
                .when(brandService).deleteBrandPermanently(404L);

        mockMvc.perform(delete("/api/brands/404"))
                .andExpect(status().isNotFound());
    }
}
