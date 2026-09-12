package com.example.spring_boot_project_api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.review.ReviewRequest;
import com.example.spring_boot_project_api.dto.request.review.UpdateReviewRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.review.ReviewResponse;
import com.example.spring_boot_project_api.exception.UnauthorizedException;
import com.example.spring_boot_project_api.service.ReviewService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;

@RestController
@Validated
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    private Long currentUserId() {
        return SecurityUtils.currentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }

    @Operation(summary = "Get approved reviews for a product (public)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully")
    })
    @GetMapping("/api/products/{productId}/reviews")
    public ResponseEntity<PagedResponse<ReviewResponse>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reviewService.getApprovedReviews(productId, page, size));
    }

    @Operation(summary = "Create a review for a product")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Review created successfully"),
            @ApiResponse(responseCode = "409", description = "You have already reviewed this product")
    })
    @PostMapping("/api/reviews")
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService
                .createReview(currentUserId(), request.getProductId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get the authenticated user's reviews")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully")
    })
    @GetMapping("/api/reviews/my")
    public ResponseEntity<PagedResponse<ReviewResponse>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reviewService.getMyReviews(currentUserId(), page, size));
    }

    @Operation(summary = "Update the authenticated user's review")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review updated successfully"),
            @ApiResponse(responseCode = "403", description = "Not the owner of this review"),
            @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @PatchMapping("/api/reviews/{id}")
    public ResponseEntity<ReviewResponse> updateMyReview(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest request) {
        return ResponseEntity.ok(reviewService.updateMyReview(currentUserId(), id, request));
    }

    @Operation(summary = "Delete the authenticated user's review (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Review deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Not the owner of this review"),
            @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @DeleteMapping("/api/reviews/{id}")
    public ResponseEntity<Void> deleteMyReview(@PathVariable Long id) {
        reviewService.deleteMyReview(currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}