package com.example.spring_boot_project_api.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.enums.AnalyticsEventType;
import com.example.spring_boot_project_api.model.AnalyticsEvent;

@Repository
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {
    interface EventCountStat {
        AnalyticsEventType getEventType();
        Long getEventCount();
    }

    @Query("""
            select e.eventType as eventType, count(e) as eventCount
            from AnalyticsEvent e
            where e.createdAt >= :start and e.createdAt < :end
            group by e.eventType
            """)
    List<EventCountStat> countEventsBetween(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    @Query("""
            select count(distinct e.visitorId)
            from AnalyticsEvent e
            where e.eventType = :eventType
              and e.createdAt >= :start and e.createdAt < :end
            """)
    long countDistinctVisitorsBetween(@Param("eventType") AnalyticsEventType eventType,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);
}