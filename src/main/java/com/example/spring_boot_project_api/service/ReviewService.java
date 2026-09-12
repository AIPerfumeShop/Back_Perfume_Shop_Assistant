package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.review.ReviewFilterRequest;
import com.example.spring_boot_project_api.dto.request.review.ReviewRequest;
import com.example.spring_boot_project_api.dto.request.review.UpdateReviewRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.review.AdminReviewSummaryResponse;
import com.example.spring_boot_project_api.dto.response.review.ReviewResponse;

public interface ReviewService {

    //Get all reviews with filtering and pagination (admin)
    PagedResponse<AdminReviewSummaryResponse> getAllReviewsFiltered(ReviewFilterRequest filter);

    //Get review by id (admin)
    ReviewResponse getReviewByIdAdmin(Long reviewId);

    //Moderate a review - approve or reject it (admin)
    ReviewResponse moderateReview(Long reviewId, Boolean approved, String note);

    //Delete an inappropriate review (soft delete, admin)
    ReviewResponse deleteReview(Long reviewId, String reason);

    //Restore a deleted review (admin)
    ReviewResponse restoreReview(Long reviewId);

    //Create a review for a product (customer)
    ReviewResponse createReview(Long userId, Long productId, ReviewRequest request);

    //Get approved reviews for a product (public)
    PagedResponse<ReviewResponse> getApprovedReviews(Long productId, int page, int size);

    //Get the authenticated user's reviews
    PagedResponse<ReviewResponse> getMyReviews(Long userId, int page, int size);

    //Update the authenticated user's own review
    ReviewResponse updateMyReview(Long userId, Long reviewId, UpdateReviewRequest request);

    //Soft delete the authenticated user's own review
    void deleteMyReview(Long userId, Long reviewId);
}