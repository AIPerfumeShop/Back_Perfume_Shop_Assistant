package com.example.spring_boot_project_api.repository.specification;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.example.spring_boot_project_api.dto.request.customer.CustomerFilterRequest;
import com.example.spring_boot_project_api.model.User;

public class CustomerSpecification {

    private CustomerSpecification() {
    }

    public static Specification<User> fromFilter(CustomerFilterRequest filter) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (filter.hasSearch()) {
                String term = "%" + filter.getSearch().trim().toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("name")), term),
                        cb.like(cb.lower(root.get("email")), term)));
            }

            if (filter.getRole() != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("role"), filter.getRole()));
            }

            if (filter.getIsActive() != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("isActive"), filter.getIsActive()));
            }

            return predicate;
        };
    }
}
