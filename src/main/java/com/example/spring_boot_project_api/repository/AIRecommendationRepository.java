package com.example.spring_boot_project_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.AIRecommendation;

@Repository
public interface AIRecommendationRepository extends JpaRepository<AIRecommendation, Long> {
    List<AIRecommendation> findByConversationId(Long conversationId);
}