package com.example.spring_boot_project_api.dto.request.ai;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AIChatPreferences {
    @Size(max = 12)
    private List<@Size(max = 80) String> families;

    @Size(max = 20)
    private List<@Size(max = 100) String> brands;

    @Size(max = 20)
    private String gender;

    @Size(max = 20)
    private String intensity;

    @DecimalMin("0")
    private BigDecimal priceMin;

    @DecimalMin("0")
    private BigDecimal priceMax;
}
