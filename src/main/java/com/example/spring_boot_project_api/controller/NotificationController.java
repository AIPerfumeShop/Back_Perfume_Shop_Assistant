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
import com.example.spring_boot_project_api.exception.UnauthorizedException;
import com.example.spring_boot_project_api.service.NotificationService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "Get notifications of the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<PagedResponse<NotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = requireUserId();
        return ResponseEntity.ok(
                notificationService.getCustomerNotifications(userId, page, size));
    }

    @Operation(summary = "Get unread notification count of the authenticated user")
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount() {
        Long userId = requireUserId();
        return ResponseEntity.ok(notificationService.getCustomerUnreadCount(userId));
    }

    @Operation(summary = "Mark a single notification as read")
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        Long userId = requireUserId();
        notificationService.markRead(id, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Mark all notifications of the user as read")
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        Long userId = requireUserId();
        notificationService.markAllReadCustomer(userId);
        return ResponseEntity.ok().build();
    }

    private Long requireUserId() {
        return SecurityUtils.currentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }
}