package com.example.spring_boot_project_api.service.impl;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.spring_boot_project_api.dto.response.ai.SimilarPerfumeResponse;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.ProductMapper;
import com.example.spring_boot_project_api.model.FragranceProfile;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.service.SimilarPerfumeService;
import com.example.spring_boot_project_api.util.FragranceSimilarityScorer;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SimilarPerfumeServiceImpl implements SimilarPerfumeService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final FragranceSimilarityScorer similarityScorer;

    @Override
    @Transactional(readOnly = true)
    public SimilarPerfumeResponse findSimilar(Long productId) {
        Product source = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        List<SimilarPerfumeResponse.Recommendation> matches = productRepository.findAll().stream()
                .filter(p -> !Objects.equals(p.getId(), source.getId())
                        && Boolean.TRUE.equals(p.getIsActive()) && hasAvailableStock(p))
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
        return similarityScorer.score(a, b);
    }

    private String reason(Product a, Product b) {
        FragranceProfile x = a.getFragranceProfile(), y = b.getFragranceProfile();
        List<String> reasons = new ArrayList<>();
        if (x != null && y != null && same(x.getFragranceFamily(), y.getFragranceFamily()))
            reasons.add(y.getFragranceFamily() + " fragrance profile");
        if (x != null && y != null && similarityScorer.hasSharedNotes(x.getFragNotes(), y.getFragNotes())) reasons.add("shared fragrance notes");
        if (x != null && y != null && x.getIntensity() != null && x.getIntensity() == y.getIntensity()) reasons.add("similar intensity");
        if (reasons.isEmpty() && a.getCategory() != null && b.getCategory() != null
                && Objects.equals(a.getCategory().getId(), b.getCategory().getId())) reasons.add("same product category");
        return reasons.isEmpty() ? "Similar product details and price range." : "Similar " + String.join(" with ", reasons) + ".";
    }

    private boolean same(String a, String b) { return a != null && b != null && a.equalsIgnoreCase(b); }

    private boolean hasAvailableStock(Product product) {
        return product.getVariants() != null && product.getVariants().stream()
                .anyMatch(variant -> Boolean.TRUE.equals(variant.getIsActive())
                        && variant.getStock() != null && variant.getStock() > 0);
    }
}
