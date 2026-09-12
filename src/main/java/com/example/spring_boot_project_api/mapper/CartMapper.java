package com.example.spring_boot_project_api.mapper;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.cart.CartItemResponse;
import com.example.spring_boot_project_api.dto.response.cart.CartResponse;
import com.example.spring_boot_project_api.model.Cart;
import com.example.spring_boot_project_api.model.CartItem;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductImage;
import com.example.spring_boot_project_api.model.ProductVariant;

@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart, List<CartItem> items) {
        if (cart == null) {
            return null;
        }
        CartResponse response = new CartResponse();
        response.setId(cart.getId());
        response.setUserId(cart.getUser() != null ? cart.getUser().getId() : null);

        List<CartItemResponse> itemResponses = (items == null ? List.<CartItem>of() : items)
                .stream()
                .map(this::toItemResponse)
                .toList();
        response.setItems(itemResponses);

        long totalItems = (items == null ? List.<CartItem>of() : items).stream()
                .mapToLong(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                .sum();
        response.setTotalItems(totalItems);

        BigDecimal totalAmount = (items == null ? List.<CartItem>of() : items).stream()
                .map(this::itemSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalAmount(totalAmount);

        return response;
    }

    private CartItemResponse toItemResponse(CartItem item) {
        CartItemResponse response = new CartItemResponse();
        response.setId(item.getId());
        response.setQuantity(item.getQuantity());

        ProductVariant variant = item.getVariant();
        if (variant != null) {
            response.setVariantId(variant.getId());
            response.setSku(variant.getSku());
            response.setSizeMl(variant.getSizeMl());
            response.setPrice(variant.getPrice());
            if (variant.getProduct() != null) {
                response.setProductId(variant.getProduct().getId());
                response.setProductName(variant.getProduct().getName());
                response.setImageUrl(firstImage(variant.getProduct()));
            }
        }
        response.setSubtotal(itemSubtotal(item));
        return response;
    }

    private BigDecimal itemSubtotal(CartItem item) {
        if (item.getVariant() == null || item.getVariant().getPrice() == null) {
            return BigDecimal.ZERO;
        }
        int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
        return item.getVariant().getPrice().multiply(BigDecimal.valueOf(quantity));
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