package com.example.spring_boot_project_api.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.AIRecommendationClick;

@Repository
public interface AIRecommendationClickRepository extends JpaRepository<AIRecommendationClick, Long> {

    interface ProductClickStat {
        Long getProductId();
        String getProductName();
        String getBrand();
        Long getCount();
    }

    @Query("""
            SELECT rc.recommendation.product.id AS productId,
                   rc.recommendation.product.name AS productName,
                   rc.recommendation.product.brand.name AS brand,
                   COUNT(rc) AS count
            FROM AIRecommendationClick rc
            WHERE rc.clickedAt BETWEEN :start AND :end
            GROUP BY rc.recommendation.product.id,
                     rc.recommendation.product.name,
                     rc.recommendation.product.brand.name
            ORDER BY COUNT(rc) DESC
            """)
    List<ProductClickStat> findTopClickedProducts(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    @Query("SELECT COUNT(rc) FROM AIRecommendationClick rc WHERE rc.clickedAt BETWEEN :start AND :end")
    long countClicksBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT rc.user.id) FROM AIRecommendationClick rc WHERE rc.clickedAt BETWEEN :start AND :end")
    long countDistinctUsersBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}