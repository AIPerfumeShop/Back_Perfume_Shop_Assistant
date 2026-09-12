package com.example.spring_boot_project_api.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.wishlist.WishlistItemResponse;
import com.example.spring_boot_project_api.dto.response.wishlist.WishlistResponse;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductImage;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.model.Wishlist;
import com.example.spring_boot_project_api.model.WishlistItem;

@Component
public class WishlistMapper {

    public WishlistResponse toResponse(Wishlist wishlist, List<WishlistItem> items) {
        if (wishlist == null) {
            return null;
        }
        WishlistResponse response = new WishlistResponse();
        response.setId(wishlist.getId());
        response.setUserId(wishlist.getUser() != null ? wishlist.getUser().getId() : null);

        List<WishlistItemResponse> itemResponses = (items == null ? List.<WishlistItem>of() : items)
                .stream()
                .map(this::toItemResponse)
                .toList();
        response.setItems(itemResponses);
        response.setTotalItems(itemResponses.size());
        return response;
    }

    private WishlistItemResponse toItemResponse(WishlistItem item) {
        WishlistItemResponse response = new WishlistItemResponse();
        response.setId(item.getId());
        response.setAddedAt(item.getCreatedAt());

        Product product = item.getProduct();
        if (product != null) {
            response.setProductId(product.getId());
            response.setName(product.getName());
            response.setBrand(product.getBrand() != null ? product.getBrand().getName() : null);
            response.setPrice(firstActivePrice(product));
            response.setImageUrl(firstImage(product));
            response.setInStock(inStock(product));
        }
        return response;
    }

    private java.math.BigDecimal firstActivePrice(Product product) {
        if (product.getVariants() == null) {
            return null;
        }
        return product.getVariants().stream()
                .filter(variant -> Boolean.TRUE.equals(variant.getIsActive()))
                .findFirst()
                .map(ProductVariant::getPrice)
                .orElse(null);
    }

    private boolean inStock(Product product) {
        return product.getVariants() != null && product.getVariants().stream()
                .anyMatch(variant -> variant.getStock() != null && variant.getStock() > 0);
    }

    private String firstImage(Product product) {
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }
        return product.getImages().stream()
                .sorted(Comparator
                        .comparing((ProductImage img) -> Boolean.FALSE.equals(img.getIsPrimary()))
                        .thenComparing(img -> img.getDisplayOrder() == null
                                ? Integer.MAX_VALUE
                                : img.getDisplayOrder()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(null);
    }
}