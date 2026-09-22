package com.example.spring_boot_project_api.dto.request.gift;

import java.math.BigDecimal;
import java.util.List;

import com.example.spring_boot_project_api.enums.GiftKnowledgeLevel;
import com.example.spring_boot_project_api.enums.GiftOccasion;
import com.example.spring_boot_project_api.enums.GiftPersonalityVibe;
import com.example.spring_boot_project_api.enums.GiftRecipientType;
import com.example.spring_boot_project_api.enums.ScentPreference;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GiftFinderRequest {

    @NotNull(message = "recipientType is required")
    private GiftRecipientType recipientType;

    @NotNull(message = "occasion is required")
    private GiftOccasion occasion;

    private List<GiftPersonalityVibe> personalityVibes;

    @NotNull(message = "budgetMin is required")
    @DecimalMin(value = "0", message = "budgetMin must not be negative")
    private BigDecimal budgetMin;

    @NotNull(message = "budgetMax is required")
    @DecimalMin(value = "0", message = "budgetMax must not be negative")
    private BigDecimal budgetMax;

    @NotNull(message = "knowledgeLevel is required")
    private GiftKnowledgeLevel knowledgeLevel;

    private List<ScentPreference> scentPreference;
}