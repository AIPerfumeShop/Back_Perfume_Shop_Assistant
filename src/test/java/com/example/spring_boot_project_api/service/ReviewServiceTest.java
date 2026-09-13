package com.example.spring_boot_project_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.example.spring_boot_project_api.dto.request.review.ReviewFilterRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.review.AdminReviewSummaryResponse;
import com.example.spring_boot_project_api.dto.response.review.ReviewResponse;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.ReviewMapper;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.Review;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.repository.ReviewRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.impl.ReviewServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    private ReviewServiceImpl reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl(
                reviewRepository, new ReviewMapper(), productRepository, userRepository);
    }

    private Review review(Long id, boolean approved, boolean deleted) {
        User user = new User();
        user.setId(1L);
        user.setName("Chan Dara");

        Product product = new Product();
        product.setId(2L);
        product.setName("Idole");

        Review review = new Review();
        review.setId(id);
        review.setUser(user);
        review.setProduct(product);
        review.setRating(5);
        review.setComment("Great scent, long lasting.");
        review.setIsApproved(approved);
        review.setIsDeleted(deleted);
        review.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        review.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        return review;
    }

    // ---------- list ----------

    @Test
    void getAllReviewsFiltered_returnsPaged() {
        Review review = review(1L, true, false);
        Page<Review> page = new PageImpl<>(List.of(review));
        when(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<AdminReviewSummaryResponse> response =
                reviewService.getAllReviewsFiltered(new ReviewFilterRequest());

        assertNotNull(response);
        assertEquals(1, response.getData().size());
        AdminReviewSummaryResponse summary = response.getData().get(0);
        assertEquals(1L, summary.getId());
        assertEquals("Chan Dara", summary.getUserName());
        assertEquals("Idole", summary.getProductName());
        assertEquals(5, summary.getRating());
        assertTrue(summary.getApproved());
        assertFalse(summary.getDeleted());
    }

    @Test
    void getAllReviewsFiltered_nullFilter_works() {
        when(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PagedResponse<AdminReviewSummaryResponse> response =
                reviewService.getAllReviewsFiltered(null);

        assertTrue(response.getData().isEmpty());
    }

    // ---------- get by id ----------

    @Test
    void getReviewByIdAdmin_returnsResponse() {
        Review review = review(7L, true, false);
        when(reviewRepository.findById(7L)).thenReturn(Optional.of(review));

        ReviewResponse response = reviewService.getReviewByIdAdmin(7L);

        assertEquals(7L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals(2L, response.getProductId());
        assertEquals("Great scent, long lasting.", response.getComment());
    }

    @Test
    void getReviewByIdAdmin_notFound_throws() {
        when(reviewRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.getReviewByIdAdmin(404L));
    }

    // ---------- moderation ----------

    @Test
    void moderateReview_approves() {
        Review review = review(1L, false, false);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse response = reviewService.moderateReview(1L, true, "Looks good");

        assertTrue(response.getApproved());
        assertEquals("Looks good", response.getModerationNote());
        verify(reviewRepository).save(review);
    }

    @Test
    void moderateReview_rejects() {
        Review review = review(1L, true, false);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse response = reviewService.moderateReview(1L, false, "Spam content");

        assertFalse(response.getApproved());
        assertEquals("Spam content", response.getModerationNote());
    }

    @Test
    void moderateReview_deletedReview_throws() {
        Review review = review(1L, true, true);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThrows(BadRequestException.class,
                () -> reviewService.moderateReview(1L, true, "nope"));
    }

    // ---------- delete (soft) ----------

    @Test
    void deleteReview_softDeletes() {
        Review review = review(1L, true, false);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse response = reviewService.deleteReview(1L, "Inappropriate");

        assertTrue(response.getDeleted());
        assertNotNull(response.getDeletedAt());
        assertEquals("Inappropriate", response.getModerationNote());
    }

    @Test
    void deleteReview_alreadyDeleted_throws() {
        Review review = review(1L, true, true);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThrows(BadRequestException.class,
                () -> reviewService.deleteReview(1L, "again"));
    }

    @Test
    void deleteReview_notFound_throws() {
        when(reviewRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.deleteReview(404L, "spam"));
    }

    // ---------- restore ----------

    @Test
    void restoreReview_clearsDeletedFlag() {
        Review review = review(1L, true, true);
        review.setDeletedAt(LocalDateTime.now());
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse response = reviewService.restoreReview(1L);

        assertFalse(response.getDeleted());
        assertNull(response.getDeletedAt());
    }

    @Test
    void restoreReview_notDeleted_throws() {
        Review review = review(1L, true, false);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThrows(BadRequestException.class,
                () -> reviewService.restoreReview(1L));
    }

    @Test
    void restoreReview_notFound_throws() {
        when(reviewRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.restoreReview(404L));
    }

    // ---------- getReviewByIdAdmin ----------

    @Test
    void getReviewByIdAdmin_deletedReview_returnsResponse() {
        Review review = review(7L, true, true);
        when(reviewRepository.findById(7L)).thenReturn(Optional.of(review));

        ReviewResponse response = reviewService.getReviewByIdAdmin(7L);

        assertEquals(7L, response.getId());
        assertTrue(response.getDeleted());
    }

    // ---------- moderateReview ----------

    @Test
    void moderateReview_notFound_throws() {
        when(reviewRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.moderateReview(404L, true, "ok"));
    }

    @Test
    void moderateReview_nullApproved_savesNull() {
        Review review = review(1L, false, false);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse response = reviewService.moderateReview(1L, null, null);

        assertNull(response.getApproved());
    }

    // ---------- deleteReview ----------

    @Test
    void deleteReview_nullReason_succeeds() {
        Review review = review(1L, true, false);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse response = reviewService.deleteReview(1L, null);

        assertTrue(response.getDeleted());
        assertNull(response.getModerationNote());
    }

    // ---------- getAllReviewsFiltered ----------

    @Test
    void getAllReviewsFiltered_multipleReviews() {
        Review r1 = review(1L, true, false);
        Review r2 = review(2L, false, false);
        Page<Review> page = new PageImpl<>(List.of(r1, r2));
        when(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<AdminReviewSummaryResponse> response =
                reviewService.getAllReviewsFiltered(new ReviewFilterRequest());

        assertEquals(2, response.getData().size());
    }
}