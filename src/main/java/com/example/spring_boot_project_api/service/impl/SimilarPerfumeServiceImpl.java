package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.spring_boot_project_api.dto.response.ai.SimilarPerfumeResponse;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.ProductMapper;
import com.example.spring_boot_project_api.model.FragranceProfile;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.service.SimilarPerfumeService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SimilarPerfumeServiceImpl implements SimilarPerfumeService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public SimilarPerfumeResponse findSimilar(Long productId) {
        Product source = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        List<SimilarPerfumeResponse.Recommendation> matches = productRepository.findAll().stream()
                .filter(p -> !Objects.equals(p.getId(), source.getId()) && Boolean.TRUE.equals(p.getIsActive()))
                .map(p -> Map.entry(p, score(source, p)))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<Product, Integer>comparingByValue().reversed())
                .limit(6)
                .map(entry -> SimilarPerfumeResponse.Recommendation.builder()
                        .productId(entry.getKey().getId()).similarityScore(entry.getValue())
                        .reason(reason(source, entry.getKey()))
                        .product(productMapper.toResponse(entry.getKey())).build())
                .toList();
        return SimilarPerfumeResponse.builder().sourceProduct(productMapper.toResponse(source)).similar(matches).build();
    }

    private int score(Product a, Product b) {
        int score = 0;
        FragranceProfile x = a.getFragranceProfile(), y = b.getFragranceProfile();
        if (x != null && y != null) {
            if (same(x.getFragranceFamily(), y.getFragranceFamily())) score += 55;
            if (x.getIntensity() != null && x.getIntensity() == y.getIntensity()) score += 20;
            if (x.getGender() != null && x.getGender() == y.getGender()) score += 10;
            if (tokenOverlap(x.getFragNotes(), y.getFragNotes())) score += 15;
        }
        if (a.getCategory() != null && b.getCategory() != null
                && Objects.equals(a.getCategory().getId(), b.getCategory().getId())) score += 10;
        BigDecimal pa = minPrice(a), pb = minPrice(b);
        if (pa != null && pb != null && pa.compareTo(BigDecimal.ZERO) > 0
                && pb.compareTo(pa.multiply(BigDecimal.valueOf(1.2))) <= 0) score += 5;
        return Math.min(score, 100);
    }

    private String reason(Product a, Product b) {
        FragranceProfile x = a.getFragranceProfile(), y = b.getFragranceProfile();
        List<String> reasons = new ArrayList<>();
        if (x != null && y != null && same(x.getFragranceFamily(), y.getFragranceFamily()))
            reasons.add(y.getFragranceFamily() + " fragrance profile");
        if (x != null && y != null && tokenOverlap(x.getFragNotes(), y.getFragNotes())) reasons.add("shared fragrance notes");
        if (x != null && y != null && x.getIntensity() != null && x.getIntensity() == y.getIntensity()) reasons.add("similar intensity");
        if (reasons.isEmpty() && a.getCategory() != null && b.getCategory() != null
                && Objects.equals(a.getCategory().getId(), b.getCategory().getId())) reasons.add("same product category");
        return reasons.isEmpty() ? "Similar product details and price range." : "Similar " + String.join(" with ", reasons) + ".";
    }

    private boolean same(String a, String b) { return a != null && b != null && a.equalsIgnoreCase(b); }
    private BigDecimal minPrice(Product p) {
        return p.getVariants().stream().filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                .map(ProductVariant::getPrice).filter(Objects::nonNull).min(BigDecimal::compareTo).orElse(null);
    }
    private boolean tokenOverlap(String a, String b) {
        if (a == null || b == null) return false;
        Set<String> terms = new HashSet<>(Arrays.asList(a.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")));
        return Arrays.stream(b.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .anyMatch(term -> term.length() > 2 && terms.contains(term));
    }
}
