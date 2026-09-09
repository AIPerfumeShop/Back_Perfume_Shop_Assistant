package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.review.ReviewFilterRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.review.AdminReviewSummaryResponse;
import com.example.spring_boot_project_api.dto.response.review.ReviewResponse;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.ReviewMapper;
import com.example.spring_boot_project_api.model.Review;
import com.example.spring_boot_project_api.repository.ReviewRepository;
import com.example.spring_boot_project_api.repository.specification.ReviewSpecification;
import com.example.spring_boot_project_api.service.ReviewService;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;

    public ReviewServiceImpl(
            ReviewRepository reviewRepository,
            ReviewMapper reviewMapper) {
        this.reviewRepository = reviewRepository;
        this.reviewMapper = reviewMapper;
    }

    //Get all reviews with filtering and pagination (admin)
    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AdminReviewSummaryResponse> getAllReviewsFiltered(ReviewFilterRequest filter) {
        if (filter == null) {
            filter = new ReviewFilterRequest();
        }

        Page<Review> reviews = reviewRepository.findAll(
                ReviewSpecification.fromFilter(filter),
                filter.toPageRequest());

        List<AdminReviewSummaryResponse> content =
                reviewMapper.toAdminSummaryResponseList(reviews.getContent());

        return new PagedResponse<>(
                content,
                reviews.getTotalElements(),
                reviews.getTotalPages(),
                reviews.getNumber(),
                reviews.getSize());
    }

    //Get review by id (admin)
    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReviewByIdAdmin(Long reviewId) {
        return reviewMapper.toResponse(findReview(reviewId));
    }

    //Moderate a review - approve or reject it (admin)
    @Override
    public ReviewResponse moderateReview(Long reviewId, Boolean approved, String note) {
        Review review = findReview(reviewId);

        if (Boolean.TRUE.equals(review.getIsDeleted())) {
            throw new BadRequestException(
                    "Cannot moderate a deleted review");
        }

        review.setIsApproved(approved);
        review.setModerationNote(note);

        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    //Delete an inappropriate review (soft delete, admin)
    @Override
    public ReviewResponse deleteReview(Long reviewId, String reason) {
        Review review = findReview(reviewId);

        if (Boolean.TRUE.equals(review.getIsDeleted())) {
            throw new BadRequestException(
                    "Review is already deleted");
        }

        review.setIsDeleted(true);
        review.setDeletedAt(LocalDateTime.now());
        review.setModerationNote(reason);

        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    //Restore a deleted review (admin)
    @Override
    public ReviewResponse restoreReview(Long reviewId) {
        Review review = findReview(reviewId);

        if (!Boolean.TRUE.equals(review.getIsDeleted())) {
            throw new BadRequestException(
                    "Review is not deleted");
        }

        review.setIsDeleted(false);
        review.setDeletedAt(null);

        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    //Find a review or throw 404
    private Review findReview(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Review not found with ID : " + reviewId));
    }
}
