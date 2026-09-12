package com.example.spring_boot_project_api.dto.request.wishlist;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WishlistItemRequest {
    @NotNull(message = "Product ID is required")
    private Long productId;
}