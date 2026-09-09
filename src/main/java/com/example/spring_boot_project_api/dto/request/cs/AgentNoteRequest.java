package com.example.spring_boot_project_api.dto.request.cs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentNoteRequest {
    @NotBlank(message = "Note is required")
    @Size(max = 2000, message = "Note must be under 2000 characters")
    private String note;
}