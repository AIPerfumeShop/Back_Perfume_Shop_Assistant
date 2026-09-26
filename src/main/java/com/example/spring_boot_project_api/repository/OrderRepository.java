package com.example.spring_boot_project_api.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>,
        JpaSpecificationExecutor<Order> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    List<Order> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findTop10ByOrderByCreatedAtDesc();

    /**
     * Most recent orders for a user after a cutoff, excluding cancelled ones.
     * Used by the checkout duplicate-guard to short-circuit double-taps.
     */
    List<Order> findTop10ByUserIdAndCreatedAtAfterAndStatusNotOrderByCreatedAtDesc(
            Long userId, LocalDateTime after, OrderStatus excludedStatus);

    @Query("""
            select o.user.id as userId,
                   count(o) as orderCount,
                   coalesce(sum(o.totalAmount), 0) as totalSpent
            from Order o
            where o.user.id in :userIds
              and exists (select p.id from Payment p where p.order = o and p.status = com.example.spring_boot_project_api.enums.PaymentStatus.SUCCESSFUL)
            group by o.user.id
            """)
    List<UserOrderStat> countOrdersByUserIds(@Param("userIds") Collection<Long> userIds);

    interface UserOrderStat {
        Long getUserId();
        Long getOrderCount();
        BigDecimal getTotalSpent();
    }

    long countByStatus(OrderStatus status);

    @Query("""
            select coalesce(sum(o.totalAmount), 0) from Order o
            where exists (select p.id from Payment p where p.order = o and p.status = com.example.spring_boot_project_api.enums.PaymentStatus.SUCCESSFUL)
            """)
    BigDecimal sumTotalAmount();

    @Query("""
            select coalesce(sum(o.totalAmount), 0) from Order o
            where o.createdAt >= :start and o.createdAt < :end
              and exists (select p.id from Payment p where p.order = o and p.status = com.example.spring_boot_project_api.enums.PaymentStatus.SUCCESSFUL)
            """)
    BigDecimal sumTotalAmountBetween(@Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    @Query("""
            select count(distinct p.order.id) from Payment p
            where p.order.createdAt >= :start and p.order.createdAt < :end
              and p.status = com.example.spring_boot_project_api.enums.PaymentStatus.SUCCESSFUL
            """)
    long countSuccessfulByCreatedAtBetween(@Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    @Query("""
            select o.status as status,
                   count(o) as orderCount
            from Order o
            where o.createdAt >= :start and o.createdAt < :end
            group by o.status
            """)
    List<OrderStatusStat> countOrdersByStatusInRange(@Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);

    @Query("""
            select u.name as customerName,
                   u.email as email,
                   count(distinct o.id) as orderCount,
                   sum(o.totalAmount) as totalSpent
            from Order o
            join o.user u
            where o.createdAt >= :start and o.createdAt < :end
              and o.status <> :cancelled
              and exists (select p.id from Payment p where p.order = o and p.status = com.example.spring_boot_project_api.enums.PaymentStatus.SUCCESSFUL)
            group by u.id, u.name, u.email
            order by sum(o.totalAmount) desc
            """)
    List<CustomerOrderStat> findTopCustomers(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end,
                                             @Param("cancelled") OrderStatus cancelled,
                                             Pageable pageable);

    @Query("""
            select distinct o.user.id
            from Order o
            where o.createdAt >= :start and o.createdAt < :end
              and o.status <> :cancelled
              and exists (select p.id from Payment p where p.order = o and p.status = com.example.spring_boot_project_api.enums.PaymentStatus.SUCCESSFUL)
            """)
    List<Long> findDistinctCustomerIdsBetween(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end,
                                              @Param("cancelled") OrderStatus cancelled);

    @Query("""
            select o.user.id as userId,
                   count(o.id) as orderCount
            from Order o
            where o.createdAt >= :start and o.createdAt < :end
              and o.status <> :cancelled
              and exists (select p.id from Payment p where p.order = o and p.status = com.example.spring_boot_project_api.enums.PaymentStatus.SUCCESSFUL)
            group by o.user.id
            """)
    List<CustomerOrderCountStat> findCustomerOrderCountsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("cancelled") OrderStatus cancelled);

    interface CustomerOrderCountStat {
        Long getUserId();
        Long getOrderCount();
    }

    interface OrderStatusStat {
        OrderStatus getStatus();
        Long getOrderCount();
    }

    interface CustomerOrderStat {
        String getCustomerName();
        String getEmail();
        Long getOrderCount();
        BigDecimal getTotalSpent();
    }
}
