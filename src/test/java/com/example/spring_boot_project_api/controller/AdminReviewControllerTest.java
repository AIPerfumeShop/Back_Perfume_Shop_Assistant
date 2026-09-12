package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.dto.request.review.ReviewFilterRequest;
import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.review.AdminReviewSummaryResponse;
import com.example.spring_boot_project_api.dto.response.review.ReviewResponse;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.service.ReviewService;

@WebMvcTest(AdminReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    private ReviewResponse reviewResponse() {
        ReviewResponse response = new ReviewResponse();
        response.setId(1L);
        response.setUserId(10L);
        response.setUserName("Chan Dara");
        response.setProductId(20L);
        response.setProductName("Idole");
        response.setRating(5);
        response.setComment("Great scent");
        response.setApproved(true);
        response.setDeleted(false);
        response.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        response.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        return response;
    }

    // ---------- getAllReviews ----------

    @Test
    void getAllReviews_returnsPaged() throws Exception {
        AdminReviewSummaryResponse summary = new AdminReviewSummaryResponse();
        summary.setId(1L);
        summary.setUserName("Chan Dara");
        summary.setProductName("Idole");
        summary.setRating(5);
        summary.setApproved(true);
        summary.setDeleted(false);
        when(reviewService.getAllReviewsFiltered(any(ReviewFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(summary), 1, 1, 0, 20));

        mockMvc.perform(get("/api/admin/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].userName").value("Chan Dara"))
                .andExpect(jsonPath("$.data[0].productName").value("Idole"))
                .andExpect(jsonPath("$.data[0].rating").value(5))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAllReviews_emptyResult() throws Exception {
        when(reviewService.getAllReviewsFiltered(any(ReviewFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(), 0, 0, 0, 20));

        mockMvc.perform(get("/api/admin/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getAllReviews_withRatingFilter() throws Exception {
        when(reviewService.getAllReviewsFiltered(any(ReviewFilterRequest.class)))
                .thenReturn(new PagedResponse<>(List.of(), 0, 0, 0, 20));

        mockMvc.perform(get("/api/admin/reviews").param("rating", "5"))
                .andExpect(status().isOk());
    }

    // ---------- getReviewById ----------

    @Test
    void getReviewById_returnsReview() throws Exception {
        when(reviewService.getReviewByIdAdmin(1L)).thenReturn(reviewResponse());

        mockMvc.perform(get("/api/admin/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productName").value("Idole"))
                .andExpect(jsonPath("$.comment").value("Great scent"))
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    void getReviewById_notFound_returns404() throws Exception {
        when(reviewService.getReviewByIdAdmin(404L))
                .thenThrow(new ResourceNotFoundException("Review not found with ID : 404"));

        mockMvc.perform(get("/api/admin/reviews/404"))
                .andExpect(status().isNotFound());
    }

    // ---------- moderateReview ----------

    @Test
    void moderateReview_approve() throws Exception {
        ReviewResponse approved = reviewResponse();
        approved.setApproved(true);
        approved.setModerationNote("Looks good");
        when(reviewService.moderateReview(eq(1L), eq(true), eq("Looks good")))
                .thenReturn(approved);

        mockMvc.perform(put("/api/admin/reviews/1/moderation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\": true, \"note\": \"Looks good\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(true));

        verify(reviewService).moderateReview(1L, true, "Looks good");
    }

    @Test
    void moderateReview_reject() throws Exception {
        ReviewResponse rejected = reviewResponse();
        rejected.setApproved(false);
        rejected.setModerationNote("Spam content");
        when(reviewService.moderateReview(eq(1L), eq(false), eq("Spam content")))
                .thenReturn(rejected);

        mockMvc.perform(put("/api/admin/reviews/1/moderation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\": false, \"note\": \"Spam content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(false));

        verify(reviewService).moderateReview(1L, false, "Spam content");
    }

    @Test
    void moderateReview_missingApproved_returns400() throws Exception {
        mockMvc.perform(put("/api/admin/reviews/1/moderation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\": \"no flag\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void moderateReview_notFound_returns404() throws Exception {
        when(reviewService.moderateReview(eq(404L), eq(true), any()))
                .thenThrow(new ResourceNotFoundException("Review not found"));

        mockMvc.perform(put("/api/admin/reviews/404/moderation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\": true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void moderateReview_deletedReview_returns400() throws Exception {
        when(reviewService.moderateReview(eq(1L), eq(true), any()))
                .thenThrow(new BadRequestException("Cannot moderate a deleted review"));

        mockMvc.perform(put("/api/admin/reviews/1/moderation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\": true}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- deleteReview ----------

    @Test
    void deleteReview_softDeletes() throws Exception {
        ReviewResponse deleted = reviewResponse();
        deleted.setDeleted(true);
        deleted.setDeletedAt(LocalDateTime.now());
        when(reviewService.deleteReview(1L, null)).thenReturn(deleted);

        mockMvc.perform(delete("/api/admin/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));

        verify(reviewService).deleteReview(1L, null);
    }

    @Test
    void deleteReview_withReason() throws Exception {
        ReviewResponse deleted = reviewResponse();
        deleted.setDeleted(true);
        deleted.setModerationNote("Inappropriate content");
        when(reviewService.deleteReview(1L, null)).thenReturn(deleted);

        mockMvc.perform(delete("/api/admin/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));

        verify(reviewService).deleteReview(1L, null);
    }

    @Test
    void deleteReview_alreadyDeleted_returns400() throws Exception {
        when(reviewService.deleteReview(1L, null))
                .thenThrow(new BadRequestException("Review is already deleted"));

        mockMvc.perform(delete("/api/admin/reviews/1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReview_notFound_returns404() throws Exception {
        when(reviewService.deleteReview(404L, null))
                .thenThrow(new ResourceNotFoundException("Review not found"));

        mockMvc.perform(delete("/api/admin/reviews/404"))
                .andExpect(status().isNotFound());
    }

    // ---------- restoreReview ----------

    @Test
    void restoreReview_restoresDeleted() throws Exception {
        ReviewResponse restored = reviewResponse();
        restored.setDeleted(false);
        when(reviewService.restoreReview(1L)).thenReturn(restored);

        mockMvc.perform(patch("/api/admin/reviews/1/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(false));

        verify(reviewService).restoreReview(1L);
    }

    @Test
    void restoreReview_notDeleted_returns400() throws Exception {
        when(reviewService.restoreReview(1L))
                .thenThrow(new BadRequestException("Review is not deleted"));

        mockMvc.perform(patch("/api/admin/reviews/1/restore"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void restoreReview_notFound_returns404() throws Exception {
        when(reviewService.restoreReview(404L))
                .thenThrow(new ResourceNotFoundException("Review not found"));

        mockMvc.perform(patch("/api/admin/reviews/404/restore"))
                .andExpect(status().isNotFound());
    }
}