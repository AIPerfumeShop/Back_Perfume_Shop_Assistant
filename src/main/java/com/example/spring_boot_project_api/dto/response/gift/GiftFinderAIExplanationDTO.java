package com.example.spring_boot_project_api.dto.response.gift;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GiftFinderAIExplanationDTO {

    private String summary;
    private List<String> reasons;
}