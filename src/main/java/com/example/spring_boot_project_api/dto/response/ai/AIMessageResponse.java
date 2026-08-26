package com.example.spring_boot_project_api.dto.response.ai;

import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.MessageSender;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AIMessageResponse {
    private Long id;
    private MessageSender sender;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
