package com.example.spring_boot_project_api.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.AIRecommendation;

@Repository
public interface AIRecommendationRepository extends JpaRepository<AIRecommendation, Long> {

    interface ProductRecommendationStat {
        Long getProductId();
        String getProductName();
        String getBrand();
        Long getCount();
    }

    List<AIRecommendation> findByConversationId(Long conversationId);

    @Query("""
            SELECT r.product.id AS productId,
                   r.product.name AS productName,
                   r.product.brand.name AS brand,
                   COUNT(r) AS count
            FROM AIRecommendation r
            WHERE r.createdAt BETWEEN :start AND :end
            GROUP BY r.product.id, r.product.name, r.product.brand.name
            ORDER BY COUNT(r) DESC
            """)
    List<ProductRecommendationStat> findTopRecommendedProducts(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    @Query("SELECT COUNT(r) FROM AIRecommendation r WHERE r.createdAt BETWEEN :start AND :end")
    long countRecommendationsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("Select COUNT(DISTINCT r.conversation.id) FROM AIRecommendation r WHERE r.createdAt BETWEEN :start AND :end")
    long countDistinctConversationsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT r.product.id) FROM AIRecommendation r WHERE r.createdAt BETWEEN :start AND :end")
    long countDistinctProductsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}