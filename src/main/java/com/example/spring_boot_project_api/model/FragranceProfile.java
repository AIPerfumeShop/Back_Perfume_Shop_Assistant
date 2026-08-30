package com.example.spring_boot_project_api.model;

import com.example.spring_boot_project_api.enums.Gender;
import com.example.spring_boot_project_api.enums.Intensity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "tb_fragrance_profiles")
public class FragranceProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "fragrance_family", length = 100)
    private String fragranceFamily;

    @Column(name = "frag_notes", columnDefinition = "TEXT")
    private String fragNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "intensity")
    private Intensity intensity;
}