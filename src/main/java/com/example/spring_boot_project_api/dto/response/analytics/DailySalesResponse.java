package com.example.spring_boot_project_api.dto.response.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DailySalesResponse {
    private LocalDate date;
    private BigDecimal revenue;
    private long orders;
}