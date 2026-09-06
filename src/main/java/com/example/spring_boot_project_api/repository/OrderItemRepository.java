package com.example.spring_boot_project_api.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.OrderItem;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query(value = """
            select oi.product_name as productName,
                   oi.brand as brand,
                   cast(sum(oi.quantity) as signed) as quantitySold,
                   sum(oi.subtotal) as totalRevenue
            from tb_order_items oi
            group by oi.product_name, oi.brand
            order by cast(sum(oi.quantity) as signed) desc
            limit 5
            """, nativeQuery = true)
    List<BestSellerProjection> findTop5BestSellers();

    interface BestSellerProjection {
        String getProductName();
        String getBrand();
        Long getQuantitySold();
        BigDecimal getTotalRevenue();
    }
}