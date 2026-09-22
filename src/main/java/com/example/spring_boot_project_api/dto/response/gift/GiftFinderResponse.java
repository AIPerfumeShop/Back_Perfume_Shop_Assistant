package com.example.spring_boot_project_api.dto.response.gift;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GiftFinderResponse {

    private boolean exactMatchFound;
    private String message;
    private GiftRecommendationDTO topRecommendation;
    private List<GiftRecommendationDTO> alternatives = new ArrayList<>();
    private GiftFinderAIExplanationDTO aiExplanation;
}