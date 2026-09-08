package com.example.spring_boot_project_api.repository.specification;

import java.time.LocalTime;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.example.spring_boot_project_api.dto.request.order.OrderFilterRequest;
import com.example.spring_boot_project_api.model.Order;

public class OrderSpecification {

    private OrderSpecification() {
    }

    public static Specification<Order> fromFilter(OrderFilterRequest filter) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (filter.getStatus() != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("status"), filter.getStatus()));
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
                        cb.like(cb.lower(root.get("phone")), "%" + term + "%"),
                        cb.like(cb.lower(root.get("shippingAddress")), "%" + term + "%"));
                try {
                    Long id = Long.parseLong(term);
                    searchPredicate = cb.or(searchPredicate,
                            cb.equal(root.get("id"), id),
                            cb.equal(root.get("user").get("id"), id));
                } catch (NumberFormatException ignored) {
                    // Search term is not numeric — id match skipped
                }
                predicate = cb.and(predicate, searchPredicate);
            }

            return predicate;
        };
    }
}