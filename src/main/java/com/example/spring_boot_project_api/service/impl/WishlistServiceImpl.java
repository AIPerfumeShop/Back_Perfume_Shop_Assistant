package com.example.spring_boot_project_api.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.wishlist.WishlistItemRequest;
import com.example.spring_boot_project_api.dto.response.wishlist.WishlistResponse;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.WishlistMapper;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.Wishlist;
import com.example.spring_boot_project_api.model.WishlistItem;
import com.example.spring_boot_project_api.repository.ProductRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.WishlistItemRepository;
import com.example.spring_boot_project_api.repository.WishlistRepository;
import com.example.spring_boot_project_api.service.WishlistService;

@Service
@Transactional
public class WishlistServiceImpl implements WishlistService {
    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final WishlistMapper wishlistMapper;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public WishlistServiceImpl(WishlistRepository wishlistRepository,
                               WishlistItemRepository wishlistItemRepository,
                               WishlistMapper wishlistMapper,
                               UserRepository userRepository,
                               ProductRepository productRepository) {
        this.wishlistRepository = wishlistRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.wishlistMapper = wishlistMapper;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    public WishlistResponse getWishlist(Long userId) {
        Wishlist wishlist = getOrCreateWishlist(userId);
        return wishlistMapper.toResponse(wishlist,
                wishlistItemRepository.findAllByWishlistId(wishlist.getId()));
    }

    @Override
    public WishlistResponse addItem(Long userId, WishlistItemRequest request) {
        Wishlist wishlist = getOrCreateWishlist(userId);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with ID : " + request.getProductId()));
        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new BadRequestException("This product is no longer available");
        }

        boolean alreadyExists = wishlistItemRepository
                .findByWishlistIdAndProductId(wishlist.getId(), product.getId())
                .isPresent();
        if (!alreadyExists) {
            WishlistItem item = new WishlistItem();
            item.setWishlist(wishlist);
            item.setProduct(product);
            wishlistItemRepository.save(item);
        }

        return wishlistMapper.toResponse(wishlist,
                wishlistItemRepository.findAllByWishlistId(wishlist.getId()));
    }

    @Override
    public void removeItem(Long userId, Long wishlistItemId) {
        Wishlist wishlist = getOrCreateWishlist(userId);

        WishlistItem item = wishlistItemRepository.findById(wishlistItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Wishlist item not found with ID : " + wishlistItemId));
        if (!item.getWishlist().getId().equals(wishlist.getId())) {
            throw new BadRequestException("Wishlist item does not belong to your wishlist");
        }
        wishlistItemRepository.delete(item);
    }

    @Override
    public void clearWishlist(Long userId) {
        Wishlist wishlist = getOrCreateWishlist(userId);
        wishlistItemRepository.deleteAllByWishlistId(wishlist.getId());
    }

    private Wishlist getOrCreateWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Wishlist wishlist = new Wishlist();
            wishlist.setUser(user);
            return wishlistRepository.save(wishlist);
        });
    }
}