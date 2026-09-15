package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.response.PagedResponse;
import com.example.spring_boot_project_api.dto.response.notification.NotificationResponse;
import com.example.spring_boot_project_api.dto.response.order.OrderStatusHistoryResponse;
import com.example.spring_boot_project_api.enums.NotificationType;
import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.model.Notification;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.OrderStatusHistory;
import com.example.spring_boot_project_api.repository.NotificationRepository;
import com.example.spring_boot_project_api.repository.OrderStatusHistoryRepository;
import com.example.spring_boot_project_api.service.NotificationService;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final NotificationRepository notificationRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${inventory.low-stock-threshold:5}")
    private int lowStockThreshold;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            OrderStatusHistoryRepository historyRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.historyRepository = historyRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void notifyCustomer(Long userId, NotificationType type, String title, String body, Long relatedId) {
        Notification notification = buildNotification(type, title, body, relatedId);
        notification.setUserId(userId);
        notification = notificationRepository.save(notification);
        messagingTemplate.convertAndSend(
                "/topic/notifications/" + userId, toResponse(notification));
    }

    @Override
    public void notifyAdmins(NotificationType type, String title, String body, Long relatedId) {
        Notification notification = buildNotification(type, title, body, relatedId);
        notification.setRecipientRole(ROLE_ADMIN);
        notification = notificationRepository.save(notification);
        messagingTemplate.convertAndSend(
                "/topic/admin/notifications", toResponse(notification));
    }

    @Override
    public void recordOrderPlaced(Order order) {
        saveHistory(order, OrderStatus.PENDING, null);
    }

    @Override
    public void orderStatusChanged(Order order, OrderStatus oldStatus, OrderStatus newStatus, String note) {
        if (order == null || order.getId() == null || oldStatus == newStatus) {
            return;
        }
        OrderStatusHistoryResponse history = saveHistory(order, newStatus, note);
        messagingTemplate.convertAndSend(
                "/topic/orders/" + order.getId() + "/status", history);

        if (order.getUser() != null && order.getUser().getId() != null) {
            notifyCustomer(
                    order.getUser().getId(),
                    NotificationType.ORDER_STATUS,
                    "Order #" + order.getId() + " " + humanize(newStatus),
                    "Your order is now " + humanize(newStatus),
                    order.getId());
        }
    }

    @Override
    public void stockAlert(Long variantId, String productName, int currentStock) {
        if (variantId == null
                || notificationRepository.existsByTypeAndRelatedIdAndIsReadFalse(
                        NotificationType.STOCK_ALERT, variantId)) {
            return;
        }
        String title = "Low stock: " + productName;
        String body = "Stock is below " + lowStockThreshold
                + " (currently " + currentStock + "). Please restock.";
        notifyAdmins(NotificationType.STOCK_ALERT, title, body, variantId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getCustomerNotifications(Long userId, int page, int size) {
        Page<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, buildPageRequest(page, size));
        return toPagedResponse(notifications);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getAdminNotifications(int page, int size) {
        Page<Notification> notifications = notificationRepository
                .findByRecipientRoleOrderByCreatedAtDesc(ROLE_ADMIN, buildPageRequest(page, size));
        return toPagedResponse(notifications);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCustomerUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getAdminUnreadCount() {
        return notificationRepository.countByRecipientRoleAndIsReadFalse(ROLE_ADMIN);
    }

    @Override
    public void markRead(Long notificationId, Long userId) {
        Notification notification = requireNotification(notificationId);
        if (!userId.equals(notification.getUserId())) {
            throw new ResourceNotFoundException("Notification not found");
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public void markAdminRead(Long notificationId) {
        Notification notification = requireNotification(notificationId);
        if (notification.getRecipientRole() == null) {
            throw new ResourceNotFoundException("Notification not found");
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public void markAllReadCustomer(Long userId) {
        notificationRepository.findByUserIdAndIsReadFalse(userId)
                .forEach(n -> n.setIsRead(true));
    }

    @Override
    public void markAllReadAdmin() {
        notificationRepository.findByRecipientRoleAndIsReadFalse(ROLE_ADMIN)
                .forEach(n -> n.setIsRead(true));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getOrderStatusHistory(Long orderId) {
        return historyRepository.findByOrderIdOrderByChangedAtAsc(orderId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    private OrderStatusHistoryResponse saveHistory(Order order, OrderStatus status, String note) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(status);
        history.setNote(note);
        history = historyRepository.save(history);
        return toHistoryResponse(history);
    }

    private OrderStatusHistoryResponse toHistoryResponse(OrderStatusHistory history) {
        OrderStatusHistoryResponse response = new OrderStatusHistoryResponse();
        response.setId(history.getId());
        response.setOrderId(history.getOrder().getId());
        response.setStatus(history.getStatus());
        response.setNote(history.getNote());
        response.setChangedAt(history.getChangedAt());
        return response;
    }

    private Notification buildNotification(NotificationType type, String title, String body, Long relatedId) {
        Notification notification = new Notification();
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setRelatedId(relatedId);
        notification.setIsRead(false);
        return notification;
    }

    private NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setBody(notification.getBody());
        response.setRelatedId(notification.getRelatedId());
        response.setIsRead(notification.getIsRead());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }

    private Notification requireNotification(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Notification not found with ID : " + notificationId));
    }

    private PageRequest buildPageRequest(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    private PagedResponse<NotificationResponse> toPagedResponse(Page<Notification> page) {
        List<NotificationResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new PagedResponse<>(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }

    private String humanize(OrderStatus status) {
        if (status == null) {
            return "";
        }
        String name = status.name();
        String lower = name.substring(1).toLowerCase();
        return name.charAt(0) + lower;
    }
}