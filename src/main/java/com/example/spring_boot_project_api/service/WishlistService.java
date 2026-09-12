package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.wishlist.WishlistItemRequest;
import com.example.spring_boot_project_api.dto.response.wishlist.WishlistResponse;

public interface WishlistService {
    WishlistResponse getWishlist(Long userId);

    WishlistResponse addItem(Long userId, WishlistItemRequest request);

    void removeItem(Long userId, Long wishlistItemId);

    void clearWishlist(Long userId);
}