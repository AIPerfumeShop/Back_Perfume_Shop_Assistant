package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.product.ProductFilterRequest;
import com.example.spring_boot_project_api.dto.request.product.ProductRequest;
import com.example.spring_boot_project_api.dto.request.product.ProductStockRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.product.ProductResponse;

public interface ProductService {
    PagedResponse<ProductResponse> getAllProducts(ProductFilterRequest filter);

    PagedResponse<ProductResponse> getProductsByBrand(Long brandId, ProductFilterRequest filter);

    ProductResponse getProductById(Long id);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);

    ProductResponse updateVariantStock(Long productId, Long variantId, ProductStockRequest request);
}