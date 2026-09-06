package com.example.spring_boot_project_api.dto.request.analytics;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalyticsFilterRequest {
    @DateTimeFormat(iso = ISO.DATE)
    private LocalDate from;

    @DateTimeFormat(iso = ISO.DATE)
    private LocalDate to;
}