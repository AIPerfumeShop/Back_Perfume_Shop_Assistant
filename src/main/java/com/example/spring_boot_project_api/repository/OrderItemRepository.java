package com.example.spring_boot_project_api.repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.enums.OrderStatus;
import com.example.spring_boot_project_api.model.OrderItem;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query(value = """
            select oi.product_name as productName,
                   oi.brand as brand,
                   cast(sum(oi.quantity) as signed) as quantitySold,
                   sum(oi.subtotal) as totalRevenue
            from tb_order_items oi
            join tb_orders o on o.id = oi.order_id
            where o.status <> 'CANCELLED'
            group by oi.product_name, oi.brand
            order by cast(sum(oi.quantity) as signed) desc
            limit 5
            """, nativeQuery = true)
    List<BestSellerProjection> findTop5BestSellers();

    @Query("""
            select function('date', oi.order.createdAt) as day,
                   sum(oi.subtotal) as revenue,
                   count(distinct oi.order.id) as orders
            from OrderItem oi
            where oi.order.createdAt >= :start and oi.order.createdAt < :end
              and oi.order.status <> :cancelled
            group by function('date', oi.order.createdAt)
            order by function('date', oi.order.createdAt) asc
            """)
    List<DailySalesStat> findDailySales(@Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end,
                                        @Param("cancelled") OrderStatus cancelled);

    @Query("""
            select oi.productName as productName,
                   oi.brand as brand,
                   sum(oi.quantity) as quantitySold,
                   sum(oi.subtotal) as totalRevenue
            from OrderItem oi
            where oi.order.createdAt >= :start and oi.order.createdAt < :end
              and oi.order.status <> :cancelled
            group by oi.productName, oi.brand
            order by sum(oi.quantity) desc
            """)
    List<ProductPerformanceStat> findProductPerformance(@Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end,
                                                        @Param("cancelled") OrderStatus cancelled,
                                                        Pageable pageable);

    @Query("""
            select c.name as categoryName,
                   sum(oi.quantity) as quantitySold,
                   sum(oi.subtotal) as totalRevenue
            from OrderItem oi
            join oi.variant v
            join v.product p
            join p.category c
            where oi.order.createdAt >= :start and oi.order.createdAt < :end
              and oi.order.status <> :cancelled
            group by c.name
            order by sum(oi.subtotal) desc
            """)
    List<CategoryPerformanceStat> findCategoryPerformance(@Param("start") LocalDateTime start,
                                                          @Param("end") LocalDateTime end,
                                                          @Param("cancelled") OrderStatus cancelled);

    @Query("""
            select b.name as brandName,
                   sum(oi.quantity) as quantitySold,
                   sum(oi.subtotal) as totalRevenue
            from OrderItem oi
            join oi.variant v
            join v.product p
            join p.brand b
            where oi.order.createdAt >= :start and oi.order.createdAt < :end
              and oi.order.status <> :cancelled
            group by b.name
            order by sum(oi.subtotal) desc
            """)
    List<BrandPerformanceStat> findBrandPerformance(@Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end,
                                                    @Param("cancelled") OrderStatus cancelled);

    @Query("""
            select coalesce(sum(oi.quantity), 0)
            from OrderItem oi
            where oi.order.createdAt >= :start and oi.order.createdAt < :end
              and oi.order.status <> :cancelled
            """)
    Long sumQuantityBetween(@Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end,
                            @Param("cancelled") OrderStatus cancelled);

    interface BestSellerProjection {
        String getProductName();
        String getBrand();
        Long getQuantitySold();
        BigDecimal getTotalRevenue();
    }

    interface DailySalesStat {
        Date getDay();
        BigDecimal getRevenue();
        Long getOrders();
    }

    interface ProductPerformanceStat {
        String getProductName();
        String getBrand();
        Long getQuantitySold();
        BigDecimal getTotalRevenue();
    }

    interface CategoryPerformanceStat {
        String getCategoryName();
        Long getQuantitySold();
        BigDecimal getTotalRevenue();
    }

    interface BrandPerformanceStat {
        String getBrandName();
        Long getQuantitySold();
        BigDecimal getTotalRevenue();
    }
}