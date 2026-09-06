package com.example.spring_boot_project_api.repository;

import java.time.LocalDateTime;
import java.sql.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.spring_boot_project_api.model.AIConversation;

public interface AIConversationRepository extends JpaRepository<AIConversation, Long> {
    List<AIConversation> findByUserIdOrderByUpdatedAtDesc(Long userId);

    interface ConversationTrendStat {
        Date getDay();
        Long getCount();
    }

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT c.user.id) FROM AIConversation c WHERE c.createdAt BETWEEN :start AND :end")
    long countDistinctUsersBetween(@Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    @Query("""
            select function('date', c.createdAt) as day,
                   count(c) as count
            from AIConversation c
            where c.createdAt between :start and :end
            group by function('date', c.createdAt)
            order by function('date', c.createdAt) asc
            """)
    List<ConversationTrendStat> findConversationTrend(@Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);
}