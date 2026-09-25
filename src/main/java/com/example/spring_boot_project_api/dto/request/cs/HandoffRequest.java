package com.example.spring_boot_project_api.dto.request.cs;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HandoffRequest {
    private Long conversationId;

    @Size(max = 500, message = "Reason must be under 500 characters")
    private String reason;

    @Size(max = 80, message = "Category must be under 80 characters")
    private String category;

    @Size(max = 200, message = "Subject must be under 200 characters")
    private String subject;

    @Size(max = 5000, message = "Message must be under 5000 characters")
    private String message;
}
