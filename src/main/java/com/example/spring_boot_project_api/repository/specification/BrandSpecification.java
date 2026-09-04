package com.example.spring_boot_project_api.repository.specification;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.example.spring_boot_project_api.dto.request.brand.BrandFilterRequest;
import com.example.spring_boot_project_api.model.Brand;

public class BrandSpecification {

    private BrandSpecification() {
    }

    public static Specification<Brand> fromFilter(BrandFilterRequest filter) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (filter.hasSearch()) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("name")),
                                "%" + filter.getSearch().trim().toLowerCase() + "%"));
            }

            if (filter.getIsActive() != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("isActive"), filter.getIsActive()));
            }

            return predicate;
        };
    }
}
