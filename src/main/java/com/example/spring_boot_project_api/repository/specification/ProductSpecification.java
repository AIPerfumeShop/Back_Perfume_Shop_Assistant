package com.example.spring_boot_project_api.repository.specification;
import com.example.spring_boot_project_api.dto.request.product.ProductFilterRequest;
import com.example.spring_boot_project_api.model.FragranceProfile;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.model.Review;

import java.math.BigDecimal;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> fromFilter(ProductFilterRequest filter) {
        return (root, query, cb) -> {
            query.distinct(true);
            Predicate predicate = cb.conjunction();

            //Default: only active (not soft-deleted) products. Admin can opt into inactive via isActive=false.
            if (filter.getIsActive() == null || Boolean.TRUE.equals(filter.getIsActive())) {
                predicate = cb.and(predicate, cb.isTrue(root.get("isActive")));
            } else {
                predicate = cb.and(predicate, cb.isFalse(root.get("isActive")));
            }

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
                Subquery<BigDecimal> minPrice = query.subquery(BigDecimal.class);
                Root<ProductVariant> subVariant = minPrice.from(ProductVariant.class);
                minPrice.select(cb.min(subVariant.get("price")));
                minPrice.where(cb.equal(subVariant.get("product").get("id"), root.get("id")));
                Order order = "desc".equalsIgnoreCase(filter.getDirection())
                        ? cb.desc(minPrice)
                        : cb.asc(minPrice);
                query.orderBy(order);
            }

            return predicate;
        };
    }
}