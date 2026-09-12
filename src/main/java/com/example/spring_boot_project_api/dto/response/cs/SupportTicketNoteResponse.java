package com.example.spring_boot_project_api.dto.response.cs;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupportTicketNoteResponse {
    private Long id;
    private String authorName;
    private String content;
    private LocalDateTime createdAt;
}