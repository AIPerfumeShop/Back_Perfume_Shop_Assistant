package com.example.spring_boot_project_api.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Notification> findByRecipientRoleOrderByCreatedAtDesc(String role, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    long countByRecipientRoleAndIsReadFalse(String role);

    List<Notification> findByUserIdAndIsReadFalse(Long userId);

    List<Notification> findByRecipientRoleAndIsReadFalse(String role);

    boolean existsByTypeAndRelatedIdAndIsReadFalse(
            com.example.spring_boot_project_api.enums.NotificationType type,
            Long relatedId);
}