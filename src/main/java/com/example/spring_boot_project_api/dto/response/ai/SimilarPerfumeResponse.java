package com.example.spring_boot_project_api.dto.response.ai;

import java.util.List;
import com.example.spring_boot_project_api.dto.response.product.ProductResponse;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class SimilarPerfumeResponse {
    private ProductResponse sourceProduct;
    private List<Recommendation> similar;

    @Getter @Builder
    public static class Recommendation {
        private Long productId;
        private int similarityScore;
        private String reason;
        private ProductResponse product;
    }
}
