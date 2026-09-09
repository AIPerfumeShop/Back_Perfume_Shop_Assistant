package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.review.ReviewFilterRequest;
import com.example.spring_boot_project_api.dto.request.review.ReviewModerationRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.review.AdminReviewSummaryResponse;
import com.example.spring_boot_project_api.dto.response.review.ReviewResponse;
import com.example.spring_boot_project_api.service.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/reviews")
@Validated
@RequiredArgsConstructor
public class AdminReviewController {
    private final ReviewService reviewService;

    //Get all reviews with filters + pagination
    @Operation(summary = "Get all reviews with filtering and pagination (admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<PagedResponse<AdminReviewSummaryResponse>> getAllReviews(
            @ModelAttribute ReviewFilterRequest filter) {
        PagedResponse<AdminReviewSummaryResponse> response =
                reviewService.getAllReviewsFiltered(filter);
        return ResponseEntity.ok(response);
    }

    //Get review details
    @Operation(summary = "Get review details (admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable Long id) {
        ReviewResponse response = reviewService.getReviewByIdAdmin(id);
        return ResponseEntity.ok(response);
    }

    //Moderate a review (approve/reject)
    @Operation(summary = "Moderate a review - approve or reject it (admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review moderated successfully"),
            @ApiResponse(responseCode = "404", description = "Review not found"),
            @ApiResponse(responseCode = "400", description = "Review is deleted and cannot be moderated")
    })
    @PutMapping("/{id}/moderation")
    public ResponseEntity<ReviewResponse> moderateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewModerationRequest request) {
        ReviewResponse response = reviewService.moderateReview(
                id, request.getApproved(), request.getNote());
        return ResponseEntity.ok(response);
    }

    //Delete an inappropriate review (soft delete)
    @Operation(summary = "Delete an inappropriate review (soft delete, admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Review not found"),
            @ApiResponse(responseCode = "400", description = "Review is already deleted")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ReviewResponse> deleteReview(@PathVariable Long id) {
        ReviewResponse response = reviewService.deleteReview(id, null);
        return ResponseEntity.ok(response);
    }

    //Restore a deleted review
    @Operation(summary = "Restore a deleted review (admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review restored successfully"),
            @ApiResponse(responseCode = "404", description = "Review not found"),
            @ApiResponse(responseCode = "400", description = "Review is not deleted")
    })
    @PatchMapping("/{id}/restore")
    public ResponseEntity<ReviewResponse> restoreReview(@PathVariable Long id) {
        ReviewResponse response = reviewService.restoreReview(id);
        return ResponseEntity.ok(response);
    }
}
