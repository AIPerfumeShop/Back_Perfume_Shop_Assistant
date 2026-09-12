package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.cart.AddCartItemRequest;
import com.example.spring_boot_project_api.dto.request.cart.UpdateCartItemRequest;
import com.example.spring_boot_project_api.dto.response.cart.CartResponse;

public interface CartService {
    CartResponse getCart(Long userId);

    CartResponse addItem(Long userId, AddCartItemRequest request);

    CartResponse updateItemQuantity(Long userId, Long cartItemId, UpdateCartItemRequest request);

    void removeItem(Long userId, Long cartItemId);

    void clearCart(Long userId);
}