package com.example.spring_boot_project_api.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product,Long>,JpaSpecificationExecutor<Product>{

    // Average rating + review count for a batch of product ids — avoids N+1 in listings
    @Query("select r.product.id as productId, avg(r.rating) as avgRate, count(r) as reviewCount " +
           "from Review r where r.product.id in :ids group by r.product.id")
    List<ProductRatingStat> findRatingStats(@Param("ids") Collection<Long> ids);

    interface ProductRatingStat {
        Long getProductId();
        Double getAvgRate();
        Long getReviewCount();
    }
}