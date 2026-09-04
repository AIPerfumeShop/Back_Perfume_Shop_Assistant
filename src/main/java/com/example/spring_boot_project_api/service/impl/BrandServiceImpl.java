package com.example.spring_boot_project_api.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.example.spring_boot_project_api.repository.specification.BrandSpecification;
import com.example.spring_boot_project_api.service.BrandService;

@Service
@Transactional
public class BrandServiceImpl implements BrandService {
    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    public BrandServiceImpl(
            BrandRepository brandRepository,
            BrandMapper brandMapper) {
        this.brandRepository = brandRepository;
        this.brandMapper = brandMapper;
    }

    @Override
    public BrandResponse createBrand(BrandRequest request) {
        if (brandRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException(
                    "Brand name already exists : " + request.getName());
        }

        Brand brand = brandMapper.toEntity(request);
        Brand savedBrand = brandRepository.save(brand);

        return brandMapper.toResponse(savedBrand, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<BrandResponse> getAllBrands(BrandFilterRequest filter) {
        if (filter == null) {
            filter = new BrandFilterRequest();
        }

        Boolean effectiveActiveStatus = filter.getIsActive();
        if (effectiveActiveStatus == null && !Boolean.TRUE.equals(filter.getIncludeInactive())) {
            effectiveActiveStatus = true;
        }

        BrandFilterRequest effectiveFilter = new BrandFilterRequest();
        effectiveFilter.setSearch(filter.getSearch());
        effectiveFilter.setIsActive(effectiveActiveStatus);
        effectiveFilter.setPage(filter.getPage());
        effectiveFilter.setSize(filter.getSize());
        effectiveFilter.setSort(filter.getSort());
        effectiveFilter.setDirection(filter.getDirection());

        Page<Brand> brands = brandRepository.findAll(
                BrandSpecification.fromFilter(effectiveFilter),
                effectiveFilter.toPageRequest());

        List<Long> ids = brands.getContent().stream()
                .map(Brand::getId)
                .toList();
        Map<Long, Long> countByBrandId = ids.isEmpty()
                ? Map.of()
                : brandRepository.countProductsByBrandIds(ids).stream()
                        .collect(Collectors.toMap(
                                BrandProductCount::getBrandId,
                                BrandProductCount::getProductCount));

        List<BrandResponse> content = brands.map(brand -> brandMapper.toResponse(
                brand,
                countByBrandId.getOrDefault(brand.getId(), 0L)))
                .getContent();

        return new PagedResponse<>(
                content,
                brands.getTotalElements(),
                brands.getTotalPages(),
                brands.getNumber(),
                brands.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse getBrandById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Brand not found with ID : " + id));

        if (!Boolean.TRUE.equals(brand.getIsActive())) {
            throw new ResourceNotFoundException("Brand not found with ID : " + id);
        }

        return brandMapper.toResponse(brand,
                brandRepository.countProductsByBrandId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public BrandStatisticsResponse getBrandStatistics(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Brand not found with ID : " + id));

        long totalProducts = brandRepository.countProductsByBrandId(id);
        long activeProducts = brandRepository.countActiveProductsByBrandId(id);

        BrandStatisticsResponse stats = new BrandStatisticsResponse();
        stats.setBrandId(brand.getId());
        stats.setBrandName(brand.getName());
        stats.setTotalProducts(totalProducts);
        stats.setActiveProducts(activeProducts);
        stats.setInactiveProducts(totalProducts - activeProducts);
        return stats;
    }

    @Override
    public BrandResponse updateBrand(Long id, BrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Brand not found with ID : " + id));

        if (!Boolean.TRUE.equals(brand.getIsActive())) {
            throw new ResourceNotFoundException("Brand not found with ID : " + id);
        }

        if (brandRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new BadRequestException(
                    "Brand name already exists : " + request.getName());
        }

        brandMapper.updateEntity(request, brand);
        Brand updatedBrand = brandRepository.save(brand);

        return brandMapper.toResponse(updatedBrand,
                brandRepository.countProductsByBrandId(id));
    }

    @Override
    public void activateBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Brand not found with ID : " + id));

        if (Boolean.TRUE.equals(brand.getIsActive())) {
            throw new BadRequestException("Brand is already active");
        }

        brand.setIsActive(true);
        brandRepository.save(brand);
    }

    @Override
    public void deactivateBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Brand not found with ID : " + id));

        if (!Boolean.TRUE.equals(brand.getIsActive())) {
            throw new BadRequestException("Brand is already deactivated");
        }

        brand.setIsActive(false);
        brandRepository.save(brand);
    }

    @Override
    public void deleteBrandPermanently(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Brand not found with ID : " + id));

        long productCount = brandRepository.countProductsByBrandId(id);
        if (productCount > 0) {
            throw new ConflictException(
                    "Cannot delete brand with ID : " + id +
                            " because it has " + productCount + " product(s)");
        }

        brandRepository.delete(brand);
    }
}
