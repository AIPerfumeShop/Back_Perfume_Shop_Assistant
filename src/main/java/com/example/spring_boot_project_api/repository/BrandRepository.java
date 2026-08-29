package com.example.spring_boot_project_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.Brand;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    // Check whether a brand name already exists 
    boolean existsByNameIgnoreCase(String name); 
    // Check duplicate name when updating a brand 
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}