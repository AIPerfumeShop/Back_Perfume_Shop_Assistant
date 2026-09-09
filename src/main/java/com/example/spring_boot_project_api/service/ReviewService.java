package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.review.ReviewFilterRequest;
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
}
