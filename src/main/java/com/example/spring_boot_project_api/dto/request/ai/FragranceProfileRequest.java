package com.example.spring_boot_project_api.dto.request.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FragranceProfileRequest {
    @Min(0) @Max(100) private Integer sweetness;
    @Min(0) @Max(100) private Integer floral;
    @Min(0) @Max(100) private Integer fresh;
    @Min(0) @Max(100) private Integer woody;
    @Pattern(regexp = "LIGHT|MEDIUM|STRONG", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String intensity;
    private String personality;
}
