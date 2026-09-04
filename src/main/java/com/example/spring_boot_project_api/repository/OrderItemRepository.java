package com.example.spring_boot_project_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.OrderItem;
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}