package com.example.spring_boot_project_api.dto.response.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExpenseResponse {
    private Long id;
    private String title;
    private BigDecimal amount;
    private String category;
    private String note;
    private LocalDate incurredAt;
    private LocalDateTime createdAt;
}