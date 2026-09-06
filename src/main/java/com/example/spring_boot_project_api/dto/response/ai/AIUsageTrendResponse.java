package com.example.spring_boot_project_api.dto.response.ai;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AIUsageTrendResponse {
    private LocalDate date;
    private long conversations;
    private long messages;
    private long recommendations;
    private long clicks;
}