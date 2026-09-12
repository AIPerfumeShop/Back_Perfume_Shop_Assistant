package com.example.spring_boot_project_api.dto.request.bakong;

import jakarta.validation.constraints.NotBlank;

public record CheckTransactionRequest(@NotBlank String md5) {
}