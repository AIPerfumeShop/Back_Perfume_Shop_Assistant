package com.example.spring_boot_project_api.repository.specification;

import java.time.LocalTime;
import java.time.LocalDateTime;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.jpa.domain.Specification;

import com.example.spring_boot_project_api.dto.request.order.OrderFilterRequest;
import com.example.spring_boot_project_api.model.Order;
import com.example.spring_boot_project_api.model.Payment;

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

            if (filter.getPaymentStatus() != null) {
                Subquery<Long> matchingPayment = query.subquery(Long.class);
                Root<Payment> payment = matchingPayment.from(Payment.class);
                Subquery<LocalDateTime> latestPaymentTime = matchingPayment.subquery(LocalDateTime.class);
                Root<Payment> latestPayment = latestPaymentTime.from(Payment.class);
                latestPaymentTime.select(cb.greatest(latestPayment.<LocalDateTime>get("createdAt")));
                latestPaymentTime.where(cb.equal(
                        latestPayment.get("order").get("id"),
                        payment.get("order").get("id")));
                matchingPayment.select(payment.<Long>get("id"));
                matchingPayment.where(
                        cb.equal(payment.get("order").get("id"), root.get("id")),
                        cb.equal(payment.get("status"), filter.getPaymentStatus()),
                        cb.equal(payment.get("createdAt"), latestPaymentTime));
                predicate = cb.and(predicate, cb.exists(matchingPayment));
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
