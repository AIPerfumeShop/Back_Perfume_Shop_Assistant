package com.example.spring_boot_project_api.dto.request.review;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewModerationRequest {
    @NotNull(message = "Approved flag is required")
    private Boolean approved;

    @Size(max = 500, message = "Moderation note must be under 500 characters")
    private String note;
}
