package com.example.spring_boot_project_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.spring_boot_project_api.model.AppSetting;

public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {

    Optional<AppSetting> findBySettingKey(String settingKey);

    /**
     * Atomically append a value to an existing counter row, creating it with
     * value {@code 1} when it does not exist yet. Safe across multiple app
     * instances sharing the same database.
     */
    @Modifying
    @Query(value = """
            INSERT INTO tb_app_settings (setting_key, setting_value, updated_at)
            VALUES (:key, '1', NOW())
            ON DUPLICATE KEY UPDATE setting_value = setting_value + 1,
                                    updated_at = NOW()
            """, nativeQuery = true)
    int incrementCounter(@Param("key") String settingKey);
}