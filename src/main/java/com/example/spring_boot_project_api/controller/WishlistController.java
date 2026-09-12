package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.wishlist.WishlistItemRequest;
import com.example.spring_boot_project_api.dto.response.wishlist.WishlistResponse;
import com.example.spring_boot_project_api.exception.UnauthorizedException;
import com.example.spring_boot_project_api.service.WishlistService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/wishlist")
@Validated
public class WishlistController {
    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    private Long currentUserId() {
        return SecurityUtils.currentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }

    @Operation(summary = "Get the authenticated user's wishlist")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wishlist retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<WishlistResponse> getWishlist() {
        return ResponseEntity.ok(wishlistService.getWishlist(currentUserId()));
    }

    @Operation(summary = "Add a product to the wishlist")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product added to wishlist"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PostMapping("/items")
    public ResponseEntity<WishlistResponse> addItem(@Valid @RequestBody WishlistItemRequest request) {
        return ResponseEntity.ok(wishlistService.addItem(currentUserId(), request));
    }

    @Operation(summary = "Remove an item from the wishlist")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item removed successfully")
    })
    @DeleteMapping("/items/{wishlistItemId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long wishlistItemId) {
        wishlistService.removeItem(currentUserId(), wishlistItemId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Clear the entire wishlist")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Wishlist cleared successfully")
    })
    @DeleteMapping
    public ResponseEntity<Void> clearWishlist() {
        wishlistService.clearWishlist(currentUserId());
        return ResponseEntity.noContent().build();
    }
}