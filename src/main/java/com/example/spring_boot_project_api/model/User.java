package com.example.spring_boot_project_api.model;

import java.time.LocalDateTime;

import javax.management.relation.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity
@Data
@jakarta.persistence.Table(name = "tb_users")
public class User {
    //Id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Name
    @Column(name = "fullname",nullable = false,length = 100)
    private String name;
    
    //Email
    @Column(name = "email", nullable = false, unique = true, length = 150)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 150, message = "Email must be under 150 characters")
    private String email;

    //Password
    @Column(name = "password", nullable = false, length = 255)
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 255,message = "Password must be between 8 and 255 characters")
    private String password;

    //Phone
    @Column(name = "phone", length = 30)
    private String phone;

    //Address
    @Column(name = "address",columnDefinition = "TEXT")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "role",nullable = false)
    private Role role;


    //Create_at
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    
    //Timestamp
    @PrePersist
    protected void onCreate(){
        LocalDateTime now = LocalDateTime.now();
        createAt=now;
        updateAt=now;
    }
    @PreUpdate
    protected void onUpdate(){
        updateAt = LocalDateTime.now();
    }
}
