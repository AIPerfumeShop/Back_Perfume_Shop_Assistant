package com.example.spring_boot_project_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.spring_boot_project_api.model.AIConversation;

public interface AIConversationRepository extends JpaRepository<AIConversation,Long>{
    List<AIConversation> findByUserIdOrderByUpdatedAtDesc(Long user_id);
}
