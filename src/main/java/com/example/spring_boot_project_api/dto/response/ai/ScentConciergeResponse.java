package com.example.spring_boot_project_api.dto.response.ai;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScentConciergeResponse {
    private String summary;
    private List<Match> matches;
    private int totalMatches;

    @Getter
    @Setter
    public static class Match {
        private Long productId;
        private String productName;
        private String brand;
        private BigDecimal price;
        private Double averageRate;
        private String fragranceFamily;
        private String fragNotes;
        private String reason;
        private int position;
    }
}