package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.brand.BrandFilterRequest;
import com.example.spring_boot_project_api.dto.request.brand.BrandRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.brand.BrandResponse;
import com.example.spring_boot_project_api.dto.response.brand.BrandStatisticsResponse;

public interface BrandService {
    BrandResponse createBrand(BrandRequest request);

    PagedResponse<BrandResponse> getAllBrands(BrandFilterRequest filter);

    //Get brand by id
    BrandResponse getBrandById(Long id);

    //Get brand statistics (product counts)
    BrandStatisticsResponse getBrandStatistics(Long id);
    //Update an existing brand
    BrandResponse updateBrand(Long id, BrandRequest request);
    //Active brand
    void activateBrand(Long id);
    //deactivate a brand
    void deactivateBrand(Long id);
    //Permanently delete a brand (only when no products reference it)
    void deleteBrandPermanently(Long id);


}