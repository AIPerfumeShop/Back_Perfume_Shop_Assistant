package com.example.spring_boot_project_api.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findAllByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<Payment> findAllByOrderUserIdOrderByCreatedAtDesc(Long userId);

    List<Payment> findAllByStatusAndMd5IsNotNull(PaymentStatus status);

    @Modifying
    @Query("""
            update Payment p
               set p.status = :status,
                   p.errorMessage = :errorMessage,
                   p.paidAt = :paidAt
             where p.id = :paymentId and p.status = 'PENDING'
            """)
    int transitionFromPending(@Param("paymentId") Long paymentId,
                              @Param("status") PaymentStatus status,
                              @Param("errorMessage") String errorMessage,
                              @Param("paidAt") LocalDateTime paidAt);
}