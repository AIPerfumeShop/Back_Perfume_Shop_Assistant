package com.example.spring_boot_project_api.repository;

import java.time.LocalDateTime;
import java.sql.Date;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.spring_boot_project_api.enums.MessageSender;
import com.example.spring_boot_project_api.model.AIMessage;

public interface AIMessageRepository extends JpaRepository<AIMessage, Long> {
    List<AIMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    interface PopularQuestionStat {
        String getQuestion();
        Long getTimesAsked();
    }

    interface MessageTrendStat {
        Date getDay();
        Long getCount();
    }

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(m) FROM AIMessage m WHERE m.createdAt BETWEEN :start AND :end AND m.sender = :sender")
    long countMessagesBySenderBetween(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end,
                                      @Param("sender") MessageSender sender);

    @Query("""
            select m.message as question,
                   count(m) as timesAsked
            from AIMessage m
            where m.createdAt between :start and :end
              and m.sender = :sender
            group by m.message
            order by count(m) desc
            """)
    List<PopularQuestionStat> findPopularQuestions(@Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end,
                                                   @Param("sender") MessageSender sender,
                                                   Pageable pageable);

    @Query("""
            select function('date', m.createdAt) as day,
                   count(m) as count
            from AIMessage m
            where m.createdAt between :start and :end
            group by function('date', m.createdAt)
            order by function('date', m.createdAt) asc
            """)
    List<MessageTrendStat> findMessageTrend(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);
}