package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.WishlistItem;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findAllByWishlistId(Long wishlistId);

    Optional<WishlistItem> findByWishlistIdAndProductId(Long wishlistId, Long productId);

    void deleteAllByWishlistId(Long wishlistId);
}