package com.example.spring_boot_project_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.spring_boot_project_api.model.AIMessage;

public interface AIMessageRepository extends JpaRepository<AIMessage,Long>{
    List<AIMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
