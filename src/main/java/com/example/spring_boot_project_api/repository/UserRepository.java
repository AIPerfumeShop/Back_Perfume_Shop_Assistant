package com.example.spring_boot_project_api.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.enums.Role;
import com.example.spring_boot_project_api.model.User;
@Repository
public interface UserRepository extends JpaRepository<User,Long>{
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    long countByRole(Role role);
    long countByRoleAndCreatedAtBetween(Role role, LocalDateTime start, LocalDateTime end);
}