package com.example.spring_boot_project_api.dto.request.ai;

import java.math.BigDecimal;

import com.example.spring_boot_project_api.enums.Gender;

/**
 * Structured search preferences derived from an AI chat conversation. Any
 * field may be {@code null} when the chat did not reveal it.
 */
public record AISearchPreferences(
        String search,
        String brand,
        Gender gender,
        String fragranceFamily,
        BigDecimal minPrice,
        BigDecimal maxPrice) {

    public static AISearchPreferences empty() {
        return new AISearchPreferences(null, null, null, null, null, null);
    }
}