package com.example.spring_boot_project_api.repository.specification;

import java.time.LocalTime;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.example.spring_boot_project_api.dto.request.review.ReviewFilterRequest;
import com.example.spring_boot_project_api.enums.ReviewStatus;
import com.example.spring_boot_project_api.model.Review;

public class ReviewSpecification {

    private ReviewSpecification() {
    }

    public static Specification<Review> fromFilter(ReviewFilterRequest filter) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (filter.getStatus() != null) {
                if (filter.getStatus() == ReviewStatus.DELETED) {
                    predicate = cb.and(predicate,
                            cb.isTrue(root.get("isDeleted")));
                } else {
                    predicate = cb.and(predicate,
                            cb.isFalse(root.get("isDeleted")));
                    predicate = cb.and(predicate,
                            cb.equal(root.get("isApproved"),
                                    filter.getStatus() == ReviewStatus.APPROVED));
                }
            }

            if (filter.getRating() != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("rating"), filter.getRating()));
            }

            if (filter.getFromDate() != null) {
                predicate = cb.and(predicate,
                        cb.greaterThanOrEqualTo(root.get("createdAt"),
                                filter.getFromDate().atStartOfDay()));
            }

            if (filter.getToDate() != null) {
                predicate = cb.and(predicate,
                        cb.lessThanOrEqualTo(root.get("createdAt"),
                                filter.getToDate().atTime(LocalTime.MAX)));
            }

            if (filter.hasSearch()) {
                String term = filter.getSearch().trim().toLowerCase();
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("user").get("name")), "%" + term + "%"),
                        cb.like(cb.lower(root.get("comment")), "%" + term + "%"));
                try {
                    Long id = Long.parseLong(term);
                    searchPredicate = cb.or(searchPredicate,
                            cb.equal(root.get("id"), id),
                            cb.equal(root.get("user").get("id"), id),
                            cb.equal(root.get("product").get("id"), id));
                } catch (NumberFormatException ignored) {
                    // Search term is not numeric — id match skipped
                }
                predicate = cb.and(predicate, searchPredicate);
            }

            return predicate;
        };
    }
}
