package com.example.spring_boot_project_api.dto.response.review;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminReviewSummaryResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long productId;
    private String productName;
    private Integer rating;
    private String comment;
    private Boolean approved;
    private Boolean deleted;
    private LocalDateTime createdAt;
}
