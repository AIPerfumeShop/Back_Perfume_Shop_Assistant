package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.brand.BrandRequest;
import com.example.spring_boot_project_api.dto.response.brand.BrandResponse;
import com.example.spring_boot_project_api.model.Brand;
@Component
public class BrandMapper {
    // BrandRequest → Brand
    public Brand toEntity(BrandRequest request){
        if (request == null) {
            return null;
        }
        Brand brand = new Brand();
        brand.setName(request.getName());
        brand.setDescription(request.getDescription());
        brand.setLogoUrl(request.getLogoUrl());
        brand.setIsActive(request.getIsActive());
        return brand;
    }

    //Brand -> BrandResponse
    public BrandResponse toResponse(Brand brand){
        if (brand == null) {
            return null;
        }
        BrandResponse response = new BrandResponse();

        response.setId(brand.getId());
        response.setName(brand.getName());
        response.setDescription(brand.getDescription());
        response.setLogoUrl(brand.getLogoUrl());
        response.setIsActive(brand.getIsActive());
        response.setCreatedAt(brand.getCreatedAt());
        response.setUpdatedAt(brand.getUpdatedAt());
        return response;
    }

    //Brand + product count -> BrandResponse
    public BrandResponse toResponse(Brand brand, Long productsCount){
        BrandResponse response = toResponse(brand);
        if (response != null) {
            response.setProductsCount(productsCount);
        }
        return response;
    }

    //Update existing Brand from BrandRequest
    public void updateEntity(BrandRequest request, Brand brand){
        if (request == null || brand == null) {
            return;
        }
        brand.setName(request.getName());
        brand.setDescription(request.getDescription());
        brand.setLogoUrl(request.getLogoUrl());
        brand.setIsActive(request.getIsActive());
    }
}