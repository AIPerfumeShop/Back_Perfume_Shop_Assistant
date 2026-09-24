package com.example.spring_boot_project_api.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>,
        JpaSpecificationExecutor<Review> {

    @Query("select avg(r.rating) as avgRating, count(r) as reviewCount from Review r")
    ReviewRatingStat findRatingSummary();

    @Query("select avg(r.rating) as avgRating, count(r) as reviewCount from Review r " +
            "where r.isApproved = true and r.isDeleted = false and r.createdAt between :start and :end")
    ReviewRatingStat findRatingSummaryBetween(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    Page<Review> findByProductIdAndIsApprovedTrueAndIsDeletedFalse(Long productId, Pageable pageable);

    Page<Review> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    @Query("""
            select r.id as reviewId,
                   r.rating as rating,
                   r.comment as comment,
                   p.name as productName,
                   u.name as userName
            from Review r
            join r.product p
            join r.user u
            where r.isApproved = true and r.isDeleted = false
              and r.rating <= :maxRating
              and r.comment is not null and r.comment <> ''
              and r.createdAt between :start and :end
            order by r.createdAt desc
            """)
    List<LowRatedReviewStat> findLowRatedReviews(@Param("maxRating") int maxRating,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end,
                                                 Pageable pageable);

    interface LowRatedReviewStat {
        Long getReviewId();
        Integer getRating();
        String getComment();
        String getProductName();
        String getUserName();
    }

    interface ReviewRatingStat {
        Double getAvgRating();
        Long getReviewCount();
    }
}