package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.notification.NotificationResponse;
import com.example.spring_boot_project_api.dto.response.order.OrderStatusHistoryResponse;
import com.example.spring_boot_project_api.enums.NotificationType;
import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.model.Order;

public interface NotificationService {

    void notifyCustomer(Long userId, NotificationType type, String title, String body, Long relatedId);

    void notifyAdmins(NotificationType type, String title, String body, Long relatedId);

    void recordOrderPlaced(Order order);

    void orderStatusChanged(Order order, OrderStatus oldStatus, OrderStatus newStatus, String note);

    void stockAlert(Long variantId, String productName, int currentStock);

    PagedResponse<NotificationResponse> getCustomerNotifications(Long userId, int page, int size);

    PagedResponse<NotificationResponse> getAdminNotifications(int page, int size);

    long getCustomerUnreadCount(Long userId);

    long getAdminUnreadCount();

    void markRead(Long notificationId, Long userId);

    void markAdminRead(Long notificationId);

    void markAllReadCustomer(Long userId);

    void markAllReadAdmin();

    List<OrderStatusHistoryResponse> getOrderStatusHistory(Long orderId);
}