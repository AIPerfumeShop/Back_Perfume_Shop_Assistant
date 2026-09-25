package com.example.spring_boot_project_api.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.spring_boot_project_api.model.CustomerFragranceProfile;

public interface CustomerFragranceProfileRepository extends JpaRepository<CustomerFragranceProfile, Long> {
    Optional<CustomerFragranceProfile> findByUserId(Long userId);
}
