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
}