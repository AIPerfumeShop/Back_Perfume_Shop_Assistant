package com.example.spring_boot_project_api.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.FragranceProfile;

@Repository
public interface FragranceProfileRepository extends JpaRepository<FragranceProfile, Long> {

    List<FragranceProfile> findAllByProductIdIn(Collection<Long> productIds);
}