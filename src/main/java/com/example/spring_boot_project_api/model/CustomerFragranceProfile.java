package com.example.spring_boot_project_api.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "tb_customer_fragrance_profiles")
public class CustomerFragranceProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private Integer sweetness = 50;
    private Integer floral = 50;
    private Integer fresh = 50;
    private Integer woody = 50;
    @Column(length = 30)
    private String intensity = "MEDIUM";
    @Column(length = 120)
    private String personality = "Balanced Explorer";
    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate
    void touch() { updatedAt = LocalDateTime.now(); }
}
