package com.marketplace.cart.service;

import com.marketplace.cart.client.CatalogServiceClient;
import com.marketplace.cart.domain.model.Cart;
import com.marketplace.cart.domain.model.CartItem;
import com.marketplace.cart.domain.model.CartStatus;
import com.marketplace.cart.domain.repository.CartItemRepository;
import com.marketplace.cart.domain.repository.CartRepository;
import com.marketplace.cart.dto.*;
import com.marketplace.cart.exception.InvalidQuantityException;
import com.marketplace.cart.exception.ResourceNotFoundException;
import com.marketplace.shared.dto.catalog.ProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing shopping carts.
 */
@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CatalogServiceClient catalogServiceClient;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       CatalogServiceClient catalogServiceClient) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.catalogServiceClient = catalogServiceClient;
    }

    /**
     * Get or create active cart for user.
     */
    @Transactional
    public CartResponse getOrCreateCart(UUID userId) {
        log.debug("Getting or creating cart for user: {}", userId);

        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> {
                    log.info("Creating new cart for user: {}", userId);
                    Cart newCart = new Cart(userId);
                    return cartRepository.save(newCart);
                });

        return toCartResponse(cart);
    }

    /**
     * Get cart by ID (internal use by other services).
     */
    @Transactional(readOnly = true)
    public CartResponse getCartById(UUID cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found: " + cartId));

        return toCartResponse(cart);
    }

    /**
     * Add item to cart or increment quantity if already exists.
     */
    @Transactional
    public CartResponse addItem(UUID userId, AddItemRequest request) {
        log.info("Adding item to cart for user {}: productId={}, quantity={}",
                userId, request.productId(), request.quantity());

        // Validate product via Catalog Service
        ProductResponse product = catalogServiceClient.getProductById(request.productId());
        log.debug("Product validated: {} - {}", product.id(), product.name());

        // Get or create cart
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Cart newCart = new Cart(userId);
                    return cartRepository.save(newCart);
                });

        // Check if product already in cart
        cartItemRepository.findByCartIdAndProductId(cart.getCartId(), request.productId())
                .ifPresentOrElse(
                        existingItem -> {
                            // Increment quantity
                            int newQuantity = existingItem.getQuantity() + request.quantity();
                            log.debug("Product already in cart, updating quantity: {} -> {}",
                                    existingItem.getQuantity(), newQuantity);
                            existingItem.setQuantity(newQuantity);
                            cartItemRepository.save(existingItem);
                        },
                        () -> {
                            // Create new cart item with snapshots
                            CartItem newItem = new CartItem(
                                    product.id(),
                                    generateSku(product),
                                    product.name(),
                                    product.basePrice(),
                                    "USD", // Default currency
                                    request.quantity()
                            );
                            cart.addItem(newItem);
                            cartItemRepository.save(newItem);
                            log.debug("Added new item to cart: {}", product.id());
                        }
                );

        Cart savedCart = cartRepository.save(cart);
        log.info("Item added to cart successfully for user: {}", userId);

        return toCartResponse(savedCart);
    }

    /**
     * Update cart item quantity.
     */
    @Transactional
    public CartResponse updateItemQuantity(UUID userId, UUID cartItemId, UpdateItemRequest request) {
        log.info("Updating cart item {} quantity to {} for user {}",
                cartItemId, request.quantity(), userId);

        // Get user's active cart
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active cart not found for user"));

        // Find cart item and validate it belongs to this cart
        CartItem cartItem = cartItemRepository.findByCartItemIdAndCartId(cartItemId, cart.getCartId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        // If quantity is 0, remove the item
        if (request.quantity() == 0) {
            log.debug("Quantity is 0, removing item from cart");
            cart.removeItem(cartItem);
            cartItemRepository.delete(cartItem);
        } else {
            // Update quantity
            cartItem.setQuantity(request.quantity());
            cartItemRepository.save(cartItem);
        }

        Cart savedCart = cartRepository.save(cart);
        log.info("Cart item quantity updated successfully");

        return toCartResponse(savedCart);
    }

    /**
     * Remove item from cart.
     */
    @Transactional
    public void removeItem(UUID userId, UUID cartItemId) {
        log.info("Removing cart item {} for user {}", cartItemId, userId);

        // Get user's active cart
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active cart not found for user"));

        // Find cart item and validate it belongs to this cart
        CartItem cartItem = cartItemRepository.findByCartItemIdAndCartId(cartItemId, cart.getCartId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);
        cartRepository.save(cart);

        log.info("Cart item removed successfully");
    }

    /**
     * Clear all items from cart.
     */
    @Transactional
    public void clearCart(UUID userId) {
        log.info("Clearing cart for user: {}", userId);

        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active cart not found for user"));

        cart.clearItems();
        cartRepository.save(cart);

        log.info("Cart cleared successfully for user: {}", userId);
    }

    /**
     * Checkout cart - mark as CHECKED_OUT and create new ACTIVE cart.
     */
    @Transactional
    public CheckoutResponse checkout(UUID userId) {
        log.info("Checking out cart for user: {}", userId);

        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active cart not found for user"));

        // Validate cart has items
        if (cart.getItems().isEmpty()) {
            throw new InvalidQuantityException("Cannot checkout empty cart");
        }

        // Mark cart as CHECKED_OUT
        cart.checkout();
        Cart checkedOutCart = cartRepository.save(cart);

        // Create new ACTIVE cart for user
        Cart newCart = new Cart(userId);
        cartRepository.save(newCart);

        log.info("Cart checked out successfully for user: {}", userId);

        return new CheckoutResponse(
                toCartResponse(checkedOutCart),
                "Cart checked out successfully. A new cart has been created."
        );
    }

    /**
     * Convert Cart entity to CartResponse DTO.
     */
    private CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::toCartItemResponse)
                .collect(Collectors.toList());

        int itemCount = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        BigDecimal subtotal = cart.getItems().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(
                cart.getCartId(),
                cart.getUserId(),
                cart.getStatus().name(),
                cart.getCurrency(),
                itemResponses,
                itemCount,
                subtotal,
                cart.getCreatedAt(),
                cart.getUpdatedAt()
        );
    }

    /**
     * Convert CartItem entity to CartItemResponse DTO.
     */
    private CartItemResponse toCartItemResponse(CartItem item) {
        return new CartItemResponse(
                item.getCartItemId(),
                item.getProductId(),
                item.getSku(),
                item.getTitleSnapshot(),
                item.getUnitPriceSnapshot(),
                item.getCurrency(),
                item.getQuantity(),
                item.getSubtotal()
        );
    }

    /**
     * Generate SKU from product (fallback to product ID if not available).
     */
    private String generateSku(ProductResponse product) {
        // In the future, this could use product variant information
        // For now, use product ID as SKU
        return "PROD-" + product.id().toString().substring(0, 8).toUpperCase();
    }
}
