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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.example.spring_boot_project_api.dto.request.brand.BrandFilterRequest;
import com.example.spring_boot_project_api.dto.request.brand.BrandRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.brand.BrandResponse;
import com.example.spring_boot_project_api.dto.response.brand.BrandStatisticsResponse;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ConflictException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.BrandMapper;
import com.example.spring_boot_project_api.model.Brand;
import com.example.spring_boot_project_api.repository.BrandRepository;
import com.example.spring_boot_project_api.service.impl.BrandServiceImpl;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    private BrandServiceImpl brandService;

    @BeforeEach
    void setUp() {
        brandService = new BrandServiceImpl(brandRepository, new BrandMapper());
    }

    private Brand brand(Long id, String name, boolean active) {
        Brand brand = new Brand();
        brand.setId(id);
        brand.setName(name);
        brand.setDescription("Description for " + name);
        brand.setLogoUrl("https://example.com/" + name.toLowerCase() + ".png");
        brand.setIsActive(active);
        brand.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        brand.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        return brand;
    }

    // ---------- createBrand ----------

    @Test
    void createBrand_success() {
        when(brandRepository.existsByNameIgnoreCase("Lancome")).thenReturn(false);
        when(brandRepository.save(any(Brand.class)))
                .thenAnswer(inv -> {
                    Brand b = inv.getArgument(0);
                    b.setId(1L);
                    return b;
                });

        BrandRequest request = new BrandRequest();
        request.setName("Lancome");
        request.setDescription("French luxury perfume house");
        request.setLogoUrl("https://example.com/lancome.png");

        BrandResponse response = brandService.createBrand(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Lancome", response.getName());
        assertEquals("French luxury perfume house", response.getDescription());
        verify(brandRepository).save(any(Brand.class));
    }

    @Test
    void createBrand_duplicateName_throwsBadRequest() {
        when(brandRepository.existsByNameIgnoreCase("Lancome")).thenReturn(true);

        BrandRequest request = new BrandRequest();
        request.setName("Lancome");

        assertThrows(BadRequestException.class,
                () -> brandService.createBrand(request));

        verify(brandRepository, never()).save(any(Brand.class));
    }

    // ---------- getAllBrands ----------

    @Test
    void getAllBrands_returnsPaged() {
        Brand brand = brand(1L, "Lancome", true);
        Page<Brand> page = new PageImpl<>(List.of(brand));
        when(brandRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(brandRepository.countProductsByBrandIds(List.of(1L)))
                .thenReturn(List.of(new BrandRepository.BrandProductCount() {
                    @Override public Long getBrandId() { return 1L; }
                    @Override public Long getProductCount() { return 5L; }
                }));

        PagedResponse<BrandResponse> response =
                brandService.getAllBrands(new BrandFilterRequest());

        assertNotNull(response);
        assertEquals(1, response.getData().size());
        assertEquals("Lancome", response.getData().get(0).getName());
        assertEquals(5L, response.getData().get(0).getProductsCount());
    }

    @Test
    void getAllBrands_nullFilter_usesDefaults() {
        Page<Brand> page = new PageImpl<>(List.of());
        when(brandRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<BrandResponse> response = brandService.getAllBrands(null);

        assertNotNull(response);
        assertTrue(response.getData().isEmpty());
    }

    @Test
    void getAllBrands_emptyPage() {
        when(brandRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PagedResponse<BrandResponse> response =
                brandService.getAllBrands(new BrandFilterRequest());

        assertTrue(response.getData().isEmpty());
    }

    // ---------- getBrandById ----------

    @Test
    void getBrandById_success() {
        Brand brand = brand(1L, "Lancome", true);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.countProductsByBrandId(1L)).thenReturn(5L);

        BrandResponse response = brandService.getBrandById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Lancome", response.getName());
        assertEquals(5L, response.getProductsCount());
    }

    @Test
    void getBrandById_notFound_throws() {
        when(brandRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> brandService.getBrandById(404L));
    }

    @Test
    void getBrandById_inactive_throws() {
        Brand brand = brand(1L, "Lancome", false);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        assertThrows(ResourceNotFoundException.class,
                () -> brandService.getBrandById(1L));
    }

    // ---------- getBrandStatistics ----------

    @Test
    void getBrandStatistics_success() {
        Brand brand = brand(1L, "Lancome", true);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.countProductsByBrandId(1L)).thenReturn(10L);
        when(brandRepository.countActiveProductsByBrandId(1L)).thenReturn(8L);

        BrandStatisticsResponse stats = brandService.getBrandStatistics(1L);

        assertNotNull(stats);
        assertEquals(1L, stats.getBrandId());
        assertEquals("Lancome", stats.getBrandName());
        assertEquals(10L, stats.getTotalProducts());
        assertEquals(8L, stats.getActiveProducts());
        assertEquals(2L, stats.getInactiveProducts());
    }

    @Test
    void getBrandStatistics_notFound_throws() {
        when(brandRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> brandService.getBrandStatistics(404L));
    }

    // ---------- updateBrand ----------

    @Test
    void updateBrand_success() {
        Brand brand = brand(1L, "Lancome", true);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.existsByNameIgnoreCaseAndIdNot("Lancome Updated", 1L)).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenAnswer(inv -> inv.getArgument(0));
        when(brandRepository.countProductsByBrandId(1L)).thenReturn(5L);

        BrandRequest request = new BrandRequest();
        request.setName("Lancome Updated");
        request.setDescription("Updated description");

        BrandResponse response = brandService.updateBrand(1L, request);

        assertEquals("Lancome Updated", response.getName());
        verify(brandRepository).save(brand);
    }

    @Test
    void updateBrand_notFound_throws() {
        when(brandRepository.findById(404L)).thenReturn(Optional.empty());

        BrandRequest request = new BrandRequest();
        request.setName("New");

        assertThrows(ResourceNotFoundException.class,
                () -> brandService.updateBrand(404L, request));
    }

    @Test
    void updateBrand_inactive_throws() {
        Brand brand = brand(1L, "Lancome", false);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        BrandRequest request = new BrandRequest();
        request.setName("New");

        assertThrows(ResourceNotFoundException.class,
                () -> brandService.updateBrand(1L, request));
    }

    @Test
    void updateBrand_duplicateName_throws() {
        Brand brand = brand(1L, "Lancome", true);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.existsByNameIgnoreCaseAndIdNot("Duplicate", 1L)).thenReturn(true);

        BrandRequest request = new BrandRequest();
        request.setName("Duplicate");

        assertThrows(BadRequestException.class,
                () -> brandService.updateBrand(1L, request));
    }

    // ---------- activateBrand ----------

    @Test
    void activateBrand_success() {
        Brand brand = brand(1L, "Lancome", false);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.save(any(Brand.class))).thenAnswer(inv -> inv.getArgument(0));

        brandService.activateBrand(1L);

        assertTrue(brand.getIsActive());
        verify(brandRepository).save(brand);
    }

    @Test
    void activateBrand_alreadyActive_throws() {
        Brand brand = brand(1L, "Lancome", true);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        assertThrows(BadRequestException.class,
                () -> brandService.activateBrand(1L));
    }

    @Test
    void activateBrand_notFound_throws() {
        when(brandRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> brandService.activateBrand(404L));
    }

    // ---------- deactivateBrand ----------

    @Test
    void deactivateBrand_success() {
        Brand brand = brand(1L, "Lancome", true);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.save(any(Brand.class))).thenAnswer(inv -> inv.getArgument(0));

        brandService.deactivateBrand(1L);

        assertFalse(brand.getIsActive());
        verify(brandRepository).save(brand);
    }

    @Test
    void deactivateBrand_alreadyInactive_throws() {
        Brand brand = brand(1L, "Lancome", false);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        assertThrows(BadRequestException.class,
                () -> brandService.deactivateBrand(1L));
    }

    @Test
    void deactivateBrand_notFound_throws() {
        when(brandRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> brandService.deactivateBrand(404L));
    }

    // ---------- deleteBrandPermanently ----------

    @Test
    void deleteBrandPermanently_success() {
        Brand brand = brand(1L, "Lancome", true);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.countProductsByBrandId(1L)).thenReturn(0L);

        brandService.deleteBrandPermanently(1L);

        verify(brandRepository).delete(brand);
    }

    @Test
    void deleteBrandPermanently_hasProducts_throwsConflict() {
        Brand brand = brand(1L, "Lancome", true);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.countProductsByBrandId(1L)).thenReturn(5L);

        assertThrows(ConflictException.class,
                () -> brandService.deleteBrandPermanently(1L));

        verify(brandRepository, never()).delete(any(Brand.class));
    }

    @Test
    void deleteBrandPermanently_notFound_throws() {
        when(brandRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> brandService.deleteBrandPermanently(404L));
    }
}
