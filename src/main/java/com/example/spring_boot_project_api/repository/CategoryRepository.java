package com.example.spring_boot_project_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Check whether a category name already exists
    boolean existsByNameIgnoreCase(String name);
    // Check duplicate name when updating a category
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}