package com.example.spring_boot_project_api.model;

import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Entity
@Data
@Table(name = "tb_users")
public class User {
    //id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    //name
    @Column(name = "name", nullable = false, length = 100)
    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be under 100 characters")
    private String name;
    //email
    @Column(name = "email", nullable = false, unique = true, length = 150)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 150, message = "Email must be under 150 characters")
    private String email;
    //password
    @Column(name = "password", nullable = false, length = 255)
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
    private String password;

    //phone
    @Column(name = "phone", length = 30)
    private String phone;

    //role
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    //is active
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    //timestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    //before insert
    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    //before update
    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}