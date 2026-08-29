package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.brand.BrandRequest;
import com.example.spring_boot_project_api.dto.response.brand.BrandResponse;

public interface BrandService {
    //Create a new Brand
    BrandResponse createBrand(BrandRequest request);

    //Get all brands
    List<BrandResponse> getAllBrands();

    //Get brand by id
    BrandResponse getBrandById(Long id);
    //Update an existing brand
    BrandResponse updateBrand(Long id, BrandRequest request);
    //Soft Delete a brand
    void deactivateBrand(Long id);
}