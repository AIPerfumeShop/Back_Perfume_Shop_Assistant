package com.example.spring_boot_project_api.dto.response.wishlist;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WishlistItemResponse {
    private Long id;
    private Long productId;
    private String name;
    private String brand;
    private java.math.BigDecimal price;
    private String imageUrl;
    private Boolean inStock;
    private java.time.LocalDateTime addedAt;
}