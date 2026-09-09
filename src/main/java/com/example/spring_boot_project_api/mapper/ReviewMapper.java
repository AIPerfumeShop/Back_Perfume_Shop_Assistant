package com.example.spring_boot_project_api.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.review.AdminReviewSummaryResponse;
import com.example.spring_boot_project_api.dto.response.review.ReviewResponse;
import com.example.spring_boot_project_api.model.Review;

@Component
public class ReviewMapper {

    //Review -> ReviewResponse
    public ReviewResponse toResponse(Review review) {
        if (review == null) {
            return null;
        }
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        if (review.getUser() != null) {
            response.setUserId(review.getUser().getId());
            response.setUserName(review.getUser().getName());
        }
        if (review.getProduct() != null) {
            response.setProductId(review.getProduct().getId());
            response.setProductName(review.getProduct().getName());
        }
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setApproved(review.getIsApproved());
        response.setDeleted(review.getIsDeleted());
        response.setModerationNote(review.getModerationNote());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());
        response.setDeletedAt(review.getDeletedAt());
        return response;
    }

    //Review -> AdminReviewSummaryResponse
    public AdminReviewSummaryResponse toAdminSummaryResponse(Review review) {
        if (review == null) {
            return null;
        }
        AdminReviewSummaryResponse response = new AdminReviewSummaryResponse();
        response.setId(review.getId());
        if (review.getUser() != null) {
            response.setUserId(review.getUser().getId());
            response.setUserName(review.getUser().getName());
        }
        if (review.getProduct() != null) {
            response.setProductId(review.getProduct().getId());
            response.setProductName(review.getProduct().getName());
        }
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setApproved(review.getIsApproved());
        response.setDeleted(review.getIsDeleted());
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }

    //List<Review> -> List<AdminReviewSummaryResponse>
    public List<AdminReviewSummaryResponse> toAdminSummaryResponseList(List<Review> reviews) {
        return reviews.stream()
                .map(this::toAdminSummaryResponse)
                .toList();
    }
}
