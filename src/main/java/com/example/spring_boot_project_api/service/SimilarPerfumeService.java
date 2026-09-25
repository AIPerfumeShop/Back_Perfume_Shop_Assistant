package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.response.ai.SimilarPerfumeResponse;

public interface SimilarPerfumeService {
    SimilarPerfumeResponse findSimilar(Long productId);
}
