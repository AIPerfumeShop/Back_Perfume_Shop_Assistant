package com.example.spring_boot_project_api.dto.response.ai;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AIPopularQuestionResponse {
    private String question;
    private long timesAsked;
}