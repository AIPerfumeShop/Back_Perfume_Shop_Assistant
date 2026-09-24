package com.example.spring_boot_project_api.dto.response.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminAnomalyDetectionResponse {
    private LocalDate from;
    private LocalDate to;
    private LocalDate compareFrom;
    private LocalDate compareTo;
    private List<AnomalyItem> anomalies;
    private boolean aiGenerated;
    private String insight;
    private List<String> insights;

    @Getter
    @Setter
    public static class AnomalyItem {
        private String metric;
        private BigDecimal currentValue;
        private BigDecimal previousValue;
        private double changePercent;
        private String direction;
        private String severity;
        private String note;
    }
}