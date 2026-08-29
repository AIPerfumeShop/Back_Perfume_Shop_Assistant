package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.brand.BrandRequest;
import com.example.spring_boot_project_api.dto.response.brand.BrandResponse;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.BrandMapper;
import com.example.spring_boot_project_api.model.Brand;
import com.example.spring_boot_project_api.repository.BrandRepository;
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

    // Create Brand
    @Override
    public BrandResponse createBrand(BrandRequest request) {
        // Check duplicate brand name
        if (brandRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException(
                    "Brand name already exists : " + request.getName());
        }
        // Convert request DTO -> Entity
        Brand brand = brandMapper.toEntity(request);

        // Save brand
        Brand saveBrand = brandRepository.save(brand);

        // Convert Entity -> Response DTO
        return brandMapper.toResponse(saveBrand);
    }

    // Get all brands
    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> getAllBrands() {
        return brandRepository.findAll()
                .stream()
                .filter(brand -> Boolean.TRUE.equals(brand.getIsActive()))
                .map(brandMapper::toResponse)
                .toList();

    }

    //Get Brand By Id
    @Override
    @Transactional(readOnly = true)
    public BrandResponse getBrandById(Long id){
        Brand brand = brandRepository.findById(id)
                    .orElseThrow( () -> 
                    new ResourceNotFoundException("Brand not found with ID : " + id)
                );
    //Do not return soft-deleted brands
    if (!Boolean.TRUE.equals(brand.getIsActive())) {
        throw new ResourceNotFoundException("Brand not found with ID : " + id);
    }
    return brandMapper.toResponse(brand);
    }

    //Update Brand
    @Override
    public BrandResponse updateBrand(
            Long id, BrandRequest request) {
        //find existing brand
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Brand not found with ID : " + id));
        //Do not update a deactivate Brand
        if (!Boolean.TRUE.equals(brand.getIsActive())) {
            throw new ResourceNotFoundException("Brand not found with ID : " + id);
        }
        //Update existing Entity
        brandMapper.updateEntity(request, brand);
        //save update brand
        Brand updateBrand = brandRepository.save(brand);
        //return response
        return brandMapper.toResponse(updateBrand);
    }

    //Soft Delete
    @Override
    public void deactivateBrand(Long id) {
        //Find existing brand
        Brand brand = brandRepository.findById(id)
                .orElseThrow( () -> 
                    new ResourceNotFoundException("Brand not found with ID : " +id));
    //Check if alread deactivated
    if(!Boolean.TRUE.equals(brand.getIsActive())){
        throw new BadRequestException("Brand is already deactivated");
    }
    //Soft Delete
    brand.setIsActive(false);

    //Save Change
    brandRepository.save(brand);
    }
}