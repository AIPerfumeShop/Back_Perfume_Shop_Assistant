package com.example.spring_boot_project_api.repository;

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

    interface ReviewRatingStat {
        Double getAvgRating();
        Long getReviewCount();
    }
}