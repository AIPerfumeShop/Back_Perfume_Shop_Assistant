package com.example.spring_boot_project_api.dto.response.ai;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCustomerPainPointsResponse {
    private LocalDate from;
    private LocalDate to;
    private long totalReviews;
    private Double averageRating;
    private List<LowRatedReviewItem> lowRatedReviews;
    private long openTickets;
    private long resolvedTickets;
    private List<String> topTicketReasons;
    private List<String> popularQuestions;
    private boolean aiGenerated;
    private String insight;
    private List<String> insights;

    @Getter
    @Setter
    public static class LowRatedReviewItem {
        private Long reviewId;
        private Integer rating;
        private String comment;
        private String productName;
        private String userName;
    }
}