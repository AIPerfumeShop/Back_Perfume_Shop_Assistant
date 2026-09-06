package com.example.spring_boot_project_api.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findTop10ByOrderByCreatedAtDesc();

    long countByStatus(OrderStatus status);

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o")
    BigDecimal sumTotalAmount();

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o " +
            "where o.createdAt >= :start and o.createdAt < :end")
    BigDecimal sumTotalAmountBetween(@Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

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
            group by u.id, u.name, u.email
            order by sum(o.totalAmount) desc
            """)
    List<CustomerOrderStat> findTopCustomers(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end,
                                             Pageable pageable);

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