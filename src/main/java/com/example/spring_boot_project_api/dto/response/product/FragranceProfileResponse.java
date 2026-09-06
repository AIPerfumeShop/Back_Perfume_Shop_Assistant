package com.example.spring_boot_project_api.dto.response.product;

import com.example.spring_boot_project_api.enums.Gender;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FragranceProfileResponse {
    private Long id;
    private Long productId;
    private Gender gender;
    private String fragranceFamily;
    private String fragNotes;
    private String intensity;
}
