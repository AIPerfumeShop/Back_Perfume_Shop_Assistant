package com.example.spring_boot_project_api.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>,
        JpaSpecificationExecutor<Review> {

    @Query("select avg(r.rating) as avgRating, count(r) as reviewCount from Review r")
    ReviewRatingStat findRatingSummary();

    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    Page<Review> findByProductIdAndIsApprovedTrueAndIsDeletedFalse(Long productId, Pageable pageable);

    Page<Review> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    interface ReviewRatingStat {
        Double getAvgRating();
        Long getReviewCount();
    }
}