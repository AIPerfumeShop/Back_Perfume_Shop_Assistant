package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.review.ReviewFilterRequest;
import com.example.spring_boot_project_api.dto.request.review.ReviewRequest;
import com.example.spring_boot_project_api.dto.request.review.UpdateReviewRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.review.AdminReviewSummaryResponse;
import com.example.spring_boot_project_api.dto.response.review.ReviewResponse;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ConflictException;
import com.example.spring_boot_project_api.exception.ForbiddenException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.ReviewMapper;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.Review;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.repository.ReviewRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.specification.ReviewSpecification;
import com.example.spring_boot_project_api.service.ReviewService;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ReviewServiceImpl(
            ReviewRepository reviewRepository,
            ReviewMapper reviewMapper,
            ProductRepository productRepository,
            UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.reviewMapper = reviewMapper;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
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

    //Create a review for a product (customer)
    @Override
    public ReviewResponse createReview(Long userId, Long productId, ReviewRequest request) {
        Product product = productRepository.findById(productId)
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with ID : " + productId));

        reviewRepository.findByUserIdAndProductId(userId, productId)
                .ifPresent(existing -> {
                    throw new ConflictException("You have already reviewed this product");
                });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setIsApproved(true);
        review.setIsDeleted(false);
        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    //Get approved reviews for a product (public)
    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getApprovedReviews(Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size > 0 ? Math.min(size, 50) : 20,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Review> reviews = reviewRepository
                .findByProductIdAndIsApprovedTrueAndIsDeletedFalse(productId, pageable);

        List<ReviewResponse> content = reviews.getContent().stream()
                .map(reviewMapper::toResponse)
                .toList();

        return new PagedResponse<>(
                content,
                reviews.getTotalElements(),
                reviews.getTotalPages(),
                reviews.getNumber(),
                reviews.getSize());
    }

    //Get the authenticated user's reviews
    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getMyReviews(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size > 0 ? Math.min(size, 50) : 20,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Review> reviews = reviewRepository
                .findByUserIdAndIsDeletedFalse(userId, pageable);

        List<ReviewResponse> content = reviews.getContent().stream()
                .map(reviewMapper::toResponse)
                .toList();

        return new PagedResponse<>(
                content,
                reviews.getTotalElements(),
                reviews.getTotalPages(),
                reviews.getNumber(),
                reviews.getSize());
    }

    //Update the authenticated user's own review
    @Override
    public ReviewResponse updateMyReview(Long userId, Long reviewId, UpdateReviewRequest request) {
        Review review = findOwnedReview(reviewId, userId);

        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }

        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    //Soft delete the authenticated user's own review
    @Override
    public void deleteMyReview(Long userId, Long reviewId) {
        Review review = findOwnedReview(reviewId, userId);

        review.setIsDeleted(true);
        review.setDeletedAt(LocalDateTime.now());

        reviewRepository.save(review);
    }

    //Find a review owned by the given user or throw 404/403
    private Review findOwnedReview(Long reviewId, Long userId) {
        Review review = findReview(reviewId);

        if (!review.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only manage your own reviews");
        }
        if (Boolean.TRUE.equals(review.getIsDeleted())) {
            throw new ResourceNotFoundException(
                    "Review not found with ID : " + reviewId);
        }
        return review;
    }

    //Find a review or throw 404
    private Review findReview(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Review not found with ID : " + reviewId));
    }
}