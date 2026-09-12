package com.example.spring_boot_project_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import com.example.spring_boot_project_api.repository.BrandRepository.BrandProductCount;
import com.example.spring_boot_project_api.service.impl.BrandServiceImpl;

@ExtendWith(MockitoExtension.class)
class BrandServiceImplTest {

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private BrandMapper brandMapper;

    @InjectMocks
    private BrandServiceImpl brandService;

    private Brand newBrand(Long id, String name, boolean active) {
        Brand brand = new Brand();
        brand.setId(id);
        brand.setName(name);
        brand.setDescription("desc");
        brand.setIsActive(active);
        return brand;
    }

    private BrandRequest newRequest(String name) {
        BrandRequest request = new BrandRequest();
        request.setName(name);
        request.setDescription("desc");
        request.setIsActive(true);
        return request;
    }

    @Test
    void createBrand_success() {
        BrandRequest request = newRequest("Chanel");
        Brand brand = newBrand(1L, "Chanel", true);

        when(brandRepository.existsByNameIgnoreCase("Chanel")).thenReturn(false);
        when(brandMapper.toEntity(request)).thenReturn(brand);
        when(brandRepository.save(brand)).thenReturn(brand);
        when(brandMapper.toResponse(brand, 0L)).thenReturn(new BrandResponse());

        brandService.createBrand(request);

        verify(brandRepository).save(brand);
        verify(brandMapper).toResponse(brand, 0L);
    }

    @Test
    void createBrand_duplicateName_throwsBadRequest() {
        BrandRequest request = newRequest("Dior");

        when(brandRepository.existsByNameIgnoreCase("Dior")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> brandService.createBrand(request));
        verify(brandRepository, never()).save(any());
    }

    @Test
    void getAllBrands_defaultsToActiveOnly() {
        Brand brand = newBrand(1L, "Chanel", true);
        BrandProductCount count = new BrandProductCount() {
            @Override
            public Long getBrandId() {
                return 1L;
            }

            @Override
            public Long getProductCount() {
                return 5L;
            }
        };

        when(brandRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(brand)));
        when(brandRepository.countProductsByBrandIds(any()))
                .thenReturn(List.of(count));
        when(brandMapper.toResponse(brand, 5L)).thenReturn(new BrandResponse());

        PagedResponse<BrandResponse> result = brandService.getAllBrands(null);

        assertEquals(1L, result.getTotalElements());
        assertEquals(1, result.getData().size());
        verify(brandMapper).toResponse(brand, 5L);
    }

    @Test
    void getBrandById_success() {
        Brand brand = newBrand(1L, "Chanel", true);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.countProductsByBrandId(1L)).thenReturn(3L);
        when(brandMapper.toResponse(brand, 3L)).thenReturn(new BrandResponse());

        brandService.getBrandById(1L);

        verify(brandRepository).countProductsByBrandId(1L);
    }

    @Test
    void getBrandById_inactive_throwsNotFound() {
        Brand brand = newBrand(1L, "Hidden", false);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        assertThrows(ResourceNotFoundException.class, () -> brandService.getBrandById(1L));
    }

    @Test
    void getBrandById_missing_throwsNotFound() {
        when(brandRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> brandService.getBrandById(99L));
    }

    @Test
    void getBrandStatistics_computesActiveDifference() {
        Brand brand = newBrand(1L, "Chanel", true);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.countProductsByBrandId(1L)).thenReturn(10L);
        when(brandRepository.countActiveProductsByBrandId(1L)).thenReturn(7L);

        BrandStatisticsResponse stats = brandService.getBrandStatistics(1L);

        assertEquals(10L, stats.getTotalProducts());
        assertEquals(7L, stats.getActiveProducts());
        assertEquals(3L, stats.getInactiveProducts());
    }

    @Test
    void updateBrand_success() {
        BrandRequest request = newRequest("Gucci");
        Brand brand = newBrand(1L, "Old", true);

        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.existsByNameIgnoreCaseAndIdNot("Gucci", 1L)).thenReturn(false);
        when(brandRepository.save(brand)).thenReturn(brand);
        when(brandRepository.countProductsByBrandId(1L)).thenReturn(0L);
        when(brandMapper.toResponse(brand, 0L)).thenReturn(new BrandResponse());

        brandService.updateBrand(1L, request);

        verify(brandMapper).updateEntity(request, brand);
        verify(brandRepository).save(brand);
    }

    @Test
    void updateBrand_duplicateName_throwsBadRequest() {
        BrandRequest request = newRequest("Nike");
        Brand brand = newBrand(1L, "Old", true);

        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.existsByNameIgnoreCaseAndIdNot("Nike", 1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> brandService.updateBrand(1L, request));
    }

    @Test
    void updateBrand_inactive_throwsNotFound() {
        BrandRequest request = newRequest("Nike");
        Brand brand = newBrand(1L, "Old", false);

        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        assertThrows(ResourceNotFoundException.class, () -> brandService.updateBrand(1L, request));
    }

    @Test
    void activateBrand_success() {
        Brand brand = newBrand(1L, "Chanel", false);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        brandService.activateBrand(1L);

        assertEquals(true, brand.getIsActive());
        verify(brandRepository).save(brand);
    }

    @Test
    void activateBrand_alreadyActive_throwsBadRequest() {
        Brand brand = newBrand(1L, "Chanel", true);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        assertThrows(BadRequestException.class, () -> brandService.activateBrand(1L));
    }

    @Test
    void deactivateBrand_success() {
        Brand brand = newBrand(1L, "Chanel", true);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        brandService.deactivateBrand(1L);

        assertEquals(false, brand.getIsActive());
        verify(brandRepository).save(brand);
    }

    @Test
    void deactivateBrand_alreadyInactive_throwsBadRequest() {
        Brand brand = newBrand(1L, "Chanel", false);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        assertThrows(BadRequestException.class, () -> brandService.deactivateBrand(1L));
    }

    @Test
    void deleteBrandPermanently_success() {
        Brand brand = newBrand(1L, "Chanel", true);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.countProductsByBrandId(1L)).thenReturn(0L);

        brandService.deleteBrandPermanently(1L);

        verify(brandRepository).delete(brand);
    }

    @Test
    void deleteBrandPermanently_withProducts_throwsConflict() {
        Brand brand = newBrand(1L, "Chanel", true);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.countProductsByBrandId(1L)).thenReturn(4L);

        assertThrows(ConflictException.class, () -> brandService.deleteBrandPermanently(1L));
        verify(brandRepository, never()).delete((Brand) any());
    }
}