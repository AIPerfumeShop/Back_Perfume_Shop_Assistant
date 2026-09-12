package com.example.spring_boot_project_api.dto.response.cs;

import java.time.LocalDateTime;
import java.util.List;

import com.example.spring_boot_project_api.dto.response.ai.AIMessageResponse;
import com.example.spring_boot_project_api.dto.response.order.OrderResponse;
import com.example.spring_boot_project_api.enums.TicketPriority;
import com.example.spring_boot_project_api.enums.TicketStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupportTicketDetailResponse {
    private Long id;
    private String ticketNumber;
    private Long userId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private TicketStatus status;
    private TicketPriority priority;
    private Long orderId;
    private Long agentId;
    private String agentName;
    private String summary;
    private String reason;
    private Long conversationId;
    private OrderResponse order;
    private List<AIMessageResponse> messages;
    private List<SupportTicketNoteResponse> notes;
    private LocalDateTime firstRepliedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}