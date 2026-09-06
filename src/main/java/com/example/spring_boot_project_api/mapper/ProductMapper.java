package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.product.ProductResponse;
import com.example.spring_boot_project_api.model.Product;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setBrand(product.getBrand() != null ? product.getBrand().getName() : null);
        response.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
        response.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : null);
        response.setActive(product.getIsActive());
        return response;
    }
}