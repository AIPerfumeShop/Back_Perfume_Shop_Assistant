package com.example.spring_boot_project_api.util;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.model.FragranceProfile;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductVariant;

/** Shared catalog similarity calculation for similar-product and personalized recommendations. */
@Component
public class FragranceSimilarityScorer {

    public int score(Product first, Product second) {
        int score = 0;
        FragranceProfile a = first.getFragranceProfile();
        FragranceProfile b = second.getFragranceProfile();
        if (a != null && b != null) {
            if (same(a.getFragranceFamily(), b.getFragranceFamily())) score += 55;
            if (a.getIntensity() != null && a.getIntensity() == b.getIntensity()) score += 20;
            if (a.getGender() != null && a.getGender() == b.getGender()) score += 10;
            if (hasSharedNotes(a.getFragNotes(), b.getFragNotes())) score += 15;
        }
        if (first.getCategory() != null && second.getCategory() != null
                && Objects.equals(first.getCategory().getId(), second.getCategory().getId())) score += 10;
        BigDecimal firstPrice = lowestActivePrice(first);
        BigDecimal secondPrice = lowestActivePrice(second);
        if (firstPrice != null && secondPrice != null && firstPrice.signum() > 0
                && secondPrice.compareTo(firstPrice.multiply(BigDecimal.valueOf(1.2))) <= 0) score += 5;
        return Math.min(score, 100);
    }

    public boolean hasSharedNotes(String first, String second) {
        if (first == null || second == null) return false;
        Set<String> terms = Arrays.stream(first.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(term -> term.length() > 2)
                .collect(Collectors.toSet());
        return Arrays.stream(second.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .anyMatch(term -> term.length() > 2 && terms.contains(term));
    }

    private boolean same(String first, String second) {
        return first != null && second != null && first.equalsIgnoreCase(second);
    }

    private BigDecimal lowestActivePrice(Product product) {
        if (product.getVariants() == null) return null;
        return product.getVariants().stream()
                .filter(variant -> Boolean.TRUE.equals(variant.getIsActive()))
                .map(ProductVariant::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }
}
