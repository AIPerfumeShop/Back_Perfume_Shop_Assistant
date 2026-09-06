package com.example.spring_boot_project_api.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.product.ProductResponse;
import com.example.spring_boot_project_api.dto.response.product.ProductVariantResponse;
import com.example.spring_boot_project_api.model.FragranceProfile;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductImage;
import com.example.spring_boot_project_api.model.ProductVariant;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return toResponse(product, null, null);
    }

    public ProductResponse toResponse(Product product, Double averageRate, Integer reviewCount) {
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
        response.setVariant(mapVariants(product));
        response.setImages(mapImages(product));
        mapFragranceProfile(product, response);
        response.setInStock(isInStock(product));
        response.setAverageRate(averageRate);
        response.setReviewCount(reviewCount != null ? reviewCount : 0);
        return response;
    }

    private List<ProductVariantResponse> mapVariants(Product product) {
        if (product.getVariants() == null) {
            return List.of();
        }
        return product.getVariants().stream()
                .map(this::toVariantResponse)
                .toList();
    }

    private ProductVariantResponse toVariantResponse(ProductVariant variant) {
        ProductVariantResponse response = new ProductVariantResponse();
        response.setId(variant.getId());
        response.setProductId(variant.getProduct() != null ? variant.getProduct().getId() : null);
        response.setSku(variant.getSku());
        response.setSizeMl(variant.getSizeMl());
        response.setPrice(variant.getPrice());
        response.setStock(variant.getStock());
        response.setIsActive(variant.getIsActive());
        return response;
    }

    private List<String> mapImages(Product product) {
        if (product.getImages() == null) {
            return List.of();
        }
        return product.getImages().stream()
                .sorted(Comparator
                        .comparing((ProductImage img) -> Boolean.FALSE.equals(img.getIsPrimary()))
                        .thenComparing(img -> img.getDisplayOrder() == null
                                ? Integer.MAX_VALUE
                                : img.getDisplayOrder()))
                .map(ProductImage::getImageUrl)
                .toList();
    }

    private void mapFragranceProfile(Product product, ProductResponse response) {
        FragranceProfile profile = product.getFragranceProfile();
        if (profile == null) {
            return;
        }
        response.setGender(profile.getGender());
        response.setFragranceFamily(profile.getFragranceFamily());
        response.setFragNotes(profile.getFragNotes());
        response.setIntensity(profile.getIntensity() != null
                ? profile.getIntensity().name()
                : null);
    }

    private boolean isInStock(Product product) {
        return product.getVariants() != null && product.getVariants().stream()
                .anyMatch(variant -> variant.getStock() != null && variant.getStock() > 0);
    }
}