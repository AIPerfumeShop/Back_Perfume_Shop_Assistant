package com.example.spring_boot_project_api.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

@Entity
@jakarta.persistence.Table(name = "tb_brands")
@Getter
@Setter
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "description",columnDefinition="TEXT")
    private String description;

    @Column(name = "logo_url",length = 500)
    private String logoUrl;

    @Column(name = "is_active",nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at",nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at",nullable = false)
    private LocalDateTime updatedAt;
    @PrePersist
    protected void onCreate(){
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
    @PreUpdate
    protected void onUpdate(){
        updatedAt = LocalDateTime.now();
    }
    
}