package com.example.spring_boot_project_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.spring_boot_project_api.model.SupportTicketNote;

public interface SupportTicketNoteRepository extends JpaRepository<SupportTicketNote, Long> {

    List<SupportTicketNote> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}