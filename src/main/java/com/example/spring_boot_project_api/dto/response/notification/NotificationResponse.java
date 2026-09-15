package com.example.spring_boot_project_api.dto.response.notification;

import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.NotificationType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String title;
    private String body;
    private Long relatedId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}