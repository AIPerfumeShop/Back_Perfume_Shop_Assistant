package com.example.spring_boot_project_api.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.cart.AddCartItemRequest;
import com.example.spring_boot_project_api.dto.request.cart.UpdateCartItemRequest;
import com.example.spring_boot_project_api.dto.response.cart.CartResponse;
import com.example.spring_boot_project_api.exception.BadRequestException;
import com.example.spring_boot_project_api.exception.ResourceNotFoundException;
import com.example.spring_boot_project_api.mapper.CartMapper;
import com.example.spring_boot_project_api.model.Cart;
import com.example.spring_boot_project_api.model.CartItem;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.model.ProductVariant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.CartItemRepository;
import com.example.spring_boot_project_api.repository.CartRepository;
import com.example.spring_boot_project_api.repository.ProductVariantRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.CartService;

@Service
@Transactional
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           CartMapper cartMapper,
                           UserRepository userRepository,
                           ProductVariantRepository productVariantRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.cartMapper = cartMapper;
        this.userRepository = userRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @Override
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return cartMapper.toResponse(cart, cartItemRepository.findAllByCartId(cart.getId()));
    }

    @Override
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);

        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Variant not found with ID : " + request.getVariantId()));

        Product product = variant.getProduct();
        if (product == null
                || !Boolean.TRUE.equals(product.getIsActive())
                || !Boolean.TRUE.equals(variant.getIsActive())) {
            throw new BadRequestException("This product is no longer available");
        }

        int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
        CartItem existing = cartItemRepository
                .findByCartIdAndVariantId(cart.getId(), variant.getId())
                .orElse(null);

        if (existing != null) {
            int newQuantity = existing.getQuantity() + quantity;
            ensureInStock(variant, newQuantity);
            existing.setQuantity(newQuantity);
        } else {
            ensureInStock(variant, quantity);
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setVariant(variant);
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return cartMapper.toResponse(cart, cartItemRepository.findAllByCartId(cart.getId()));
    }

    @Override
    public CartResponse updateItemQuantity(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = requireCartItem(cart, cartItemId);
        ProductVariant variant = item.getVariant();
        ensureInStock(variant, request.getQuantity());
        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return cartMapper.toResponse(cart, cartItemRepository.findAllByCartId(cart.getId()));
    }

    @Override
    public void removeItem(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.delete(requireCartItem(cart, cartItemId));
    }

    @Override
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteAllByCartId(cart.getId());
    }

    private CartItem requireCartItem(Cart cart, Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found with ID : " + cartItemId));
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Cart item does not belong to your cart");
        }
        return item;
    }

    private void ensureInStock(ProductVariant variant, int quantity) {
        if (variant.getStock() != null && quantity > variant.getStock()) {
            throw new BadRequestException("Only " + variant.getStock() + " units in stock");
        }
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }
}