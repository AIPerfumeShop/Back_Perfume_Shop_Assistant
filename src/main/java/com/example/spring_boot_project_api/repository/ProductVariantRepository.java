package com.example.spring_boot_project_api.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import com.example.spring_boot_project_api.model.ProductVariant;
@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    Optional<ProductVariant> findBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariant v where v.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);

    List<ProductVariant> findAllByProductIdIn(Collection<Long> productIds);

    List<ProductVariant> findByStockLessThan(int threshold);

    List<ProductVariant> findByStockLessThanAndIsActiveTrue(int threshold);

    Page<ProductVariant> findByStockLessThan(int threshold, Pageable pageable);

    @Query("""
            SELECT v FROM ProductVariant v
            JOIN v.product p
            LEFT JOIN p.brand b
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(v.sku) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<ProductVariant> searchByProductNameBrandOrSku(
            @Param("search") String search, Pageable pageable);

    @Query("""
            SELECT v FROM ProductVariant v
            JOIN v.product p
            LEFT JOIN p.brand b
            WHERE v.stock < :threshold
              AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(v.sku) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<ProductVariant> searchLowStockByProductNameBrandOrSku(
            @Param("threshold") int threshold,
            @Param("search") String search,
            Pageable pageable);
}