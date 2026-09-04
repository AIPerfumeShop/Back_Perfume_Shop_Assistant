package com.example.spring_boot_project_api.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.Brand;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long>, JpaSpecificationExecutor<Brand> {
    // Check whether a brand name already exists 
    boolean existsByNameIgnoreCase(String name); 
    // Check duplicate name when updating a brand 
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    // Number of products (any status) referencing a brand
    @Query("select count(p) from Product p where p.brand.id = :brandId")
    long countProductsByBrandId(@Param("brandId") Long brandId);

    // Number of active products referencing a brand
    @Query("select count(p) from Product p where p.brand.id = :brandId and p.isActive = true")
    long countActiveProductsByBrandId(@Param("brandId") Long brandId);

    // Product counts for a batch of brand ids — avoids N+1 queries in listings
    @Query("select p.brand.id as brandId, count(p) as productCount " +
           "from Product p where p.brand.id in :ids group by p.brand.id")
    List<BrandProductCount> countProductsByBrandIds(@Param("ids") Collection<Long> ids);

    interface BrandProductCount {
        Long getBrandId();
        Long getProductCount();
    }
}