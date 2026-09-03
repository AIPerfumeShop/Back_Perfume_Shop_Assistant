package com.example.spring_boot_project_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.ProductVariant;
@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    Optional<ProductVariant> findBySku(String sku);
}