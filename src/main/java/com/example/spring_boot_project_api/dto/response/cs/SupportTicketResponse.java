package com.example.spring_boot_project_api.dto.response.cs;

import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.TicketPriority;
import com.example.spring_boot_project_api.enums.TicketStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupportTicketResponse {
    private Long id;
    private String ticketNumber;
    private Long userId;
    private String customerName;
    private TicketStatus status;
    private TicketPriority priority;
    private Long orderId;
    private Long agentId;
    private String agentName;
    private String summary;
    private String reason;
    private LocalDateTime firstRepliedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}