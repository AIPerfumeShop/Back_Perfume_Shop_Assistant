package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.notification.NotificationResponse;
import com.example.spring_boot_project_api.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {

    private final NotificationService notificationService;

    public AdminNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "Get admin notifications (e.g. low-stock alerts)")
    @GetMapping
    public ResponseEntity<PagedResponse<NotificationResponse>> getAdminNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                notificationService.getAdminNotifications(page, size));
    }

    @Operation(summary = "Get unread admin notification count")
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getAdminUnreadCount());
    }

    @Operation(summary = "Mark a single admin notification as read")
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        notificationService.markAdminRead(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Mark all admin notifications as read")
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllReadAdmin();
        return ResponseEntity.ok().build();
    }
}