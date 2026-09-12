package com.example.spring_boot_project_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.Settings;

@Repository
public interface SettingsRepository extends JpaRepository<Settings, Long> {
    Optional<Settings> findBySettingKey(String settingKey);

    boolean existsBySettingKey(String settingKey);

    boolean existsBySettingKeyAndIdNot(String settingKey, Long id);
}