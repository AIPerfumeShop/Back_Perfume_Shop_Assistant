package com.example.spring_boot_project_api.dto.response.cs;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupportQueueResponse {
    private long urgentCount;
    private long openCount;
    private long pendingCount;
    private long inProgressCount;
    private long resolvedCount;
    private long totalCount;
    private List<SupportTicketResponse> tickets;
}