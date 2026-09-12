package com.example.spring_boot_project_api.dto.response.cs;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupportAnalyticsResponse {
    private LocalDate from;
    private LocalDate to;
    private long createdCount;
    private long urgentCount;
    private long openCount;
    private long pendingCount;
    private long inProgressCount;
    private long resolvedCount;
    private Double avgFirstResponseMinutes;
    private Double avgResolutionMinutes;
}