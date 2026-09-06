package com.example.spring_boot_project_api.repository.specification;
import com.example.spring_boot_project_api.dto.request.product.ProductFilterRequest;
import com.example.spring_boot_project_api.model.FragranceProfile;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.model.Review;

import java.math.BigDecimal;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> fromFilter(ProductFilterRequest filter) {
        return (root, query, cb) -> {
            query.distinct(true);
            Predicate predicate = cb.conjunction();

            if (filter.hasSearch()) {
                String term = "%" + filter.getSearch().trim().toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("name")), term),
                        cb.like(cb.lower(root.get("description")), term)));
            }

            if (filter.getCategoryId() != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("category").get("id"), filter.getCategoryId()));
            }

            if (filter.getBrand() != null && !filter.getBrand().isBlank()) {
                predicate = cb.and(predicate,
                        cb.equal(cb.lower(root.get("brand").get("name")),
                                filter.getBrand().trim().toLowerCase()));
            }

            if (filter.getGender() != null) {
                Join<Product, FragranceProfile> profile = root.join("fragranceProfile");
                predicate = cb.and(predicate,
                        cb.equal(profile.get("gender"), filter.getGender()));
            }

            if (filter.getFragranceFamily() != null && !filter.getFragranceFamily().isBlank()) {
                Join<Product, FragranceProfile> profile = root.join("fragranceProfile");
                predicate = cb.and(predicate,
                        cb.equal(cb.lower(profile.get("fragranceFamily")),
                                filter.getFragranceFamily().trim().toLowerCase()));
            }

            if (filter.getMinPrice() != null || filter.getMaxPrice() != null) {
                Join<Product, ProductVariant> variant = root.join("variants");
                if (filter.getMinPrice() != null && filter.getMaxPrice() != null) {
                    predicate = cb.and(predicate,
                            cb.between(variant.get("price"),
                                    filter.getMinPrice(), filter.getMaxPrice()));
                } else if (filter.getMinPrice() != null) {
                    predicate = cb.and(predicate,
                            cb.greaterThanOrEqualTo(variant.get("price"), filter.getMinPrice()));
                } else {
                    predicate = cb.and(predicate,
                            cb.lessThanOrEqualTo(variant.get("price"), filter.getMaxPrice()));
                }
            }

            if (Boolean.TRUE.equals(filter.getInStock())) {
                Join<Product, ProductVariant> variant = root.join("variants");
                predicate = cb.and(predicate,
                        cb.greaterThan(variant.get("stock"), 0));
            }

            if (filter.getMinRate() != null) {
                Join<Product, Review> review = root.join("reviews");
                query.groupBy(root.get("id"));
                query.having(cb.greaterThanOrEqualTo(
                        cb.avg(review.get("rating")), filter.getMinRate().doubleValue()));
            }

            // 9. PRICE SORT — order by lowest variant price
            if (filter.isPriceSort()) {
                Expression<BigDecimal> lowestPrice =
                        cb.min(root.join("variants").get("price"));
                if ("desc".equalsIgnoreCase(filter.getDirection())) {
                    query.orderBy(cb.desc(lowestPrice));
                } else {
                    query.orderBy(cb.asc(lowestPrice));
                }
            }

            return predicate;
        };
    }
}