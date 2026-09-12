package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.cart.AddCartItemRequest;
import com.example.spring_boot_project_api.dto.request.cart.UpdateCartItemRequest;
import com.example.spring_boot_project_api.dto.response.cart.CartResponse;
import com.example.spring_boot_project_api.exception.UnauthorizedException;
import com.example.spring_boot_project_api.service.CartService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart")
@Validated
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    private Long currentUserId() {
        return SecurityUtils.currentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }

    @Operation(summary = "Get the authenticated user's cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        return ResponseEntity.ok(cartService.getCart(currentUserId()));
    }

    @Operation(summary = "Add an item to the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item added to cart"),
            @ApiResponse(responseCode = "404", description = "Variant not found")
    })
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(currentUserId(), request));
    }

    @Operation(summary = "Update a cart item's quantity")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantity updated successfully"),
            @ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateItemQuantity(@PathVariable Long cartItemId,
                                                           @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItemQuantity(currentUserId(), cartItemId, request));
    }

    @Operation(summary = "Remove an item from the cart")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item removed successfully")
    })
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long cartItemId) {
        cartService.removeItem(currentUserId(), cartItemId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Clear the entire cart")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cart cleared successfully")
    })
    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        cartService.clearCart(currentUserId());
        return ResponseEntity.noContent().build();
    }
}