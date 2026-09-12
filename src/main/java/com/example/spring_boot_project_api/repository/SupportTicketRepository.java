package com.example.spring_boot_project_api.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.spring_boot_project_api.enums.TicketPriority;
import com.example.spring_boot_project_api.enums.TicketStatus;
import com.example.spring_boot_project_api.model.SupportTicket;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    Page<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<SupportTicket> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<SupportTicket> findByStatusOrderByCreatedAtDesc(TicketStatus status);

    List<SupportTicket> findByStatusNotOrderByCreatedAtDesc(TicketStatus status);

    List<SupportTicket> findByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime start, LocalDateTime end);

    List<SupportTicket> findByResolvedAtBetweenOrderByResolvedAtAsc(LocalDateTime start, LocalDateTime end);

    Optional<SupportTicket> findFirstByConversationIdAndStatusNot(
            Long conversationId, TicketStatus status);

    long countByStatus(TicketStatus status);

    long countByPriorityAndStatusIn(TicketPriority priority, List<TicketStatus> statuses);

    boolean existsByTicketNumber(String ticketNumber);
}