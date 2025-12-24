package com.marketplace.cart.controller;

import com.marketplace.cart.dto.*;
import com.marketplace.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for shopping cart operations.
 */
@RestController
@RequestMapping("/api/v1/carts")
@Tag(name = "Shopping Cart", description = "APIs for managing shopping carts")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Get or create active cart for user.
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get cart", description = "Get or create active cart for user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart retrieved or created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID")
    })
    public ResponseEntity<CartResponse> getCart(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId) {

        log.debug("Get cart request for user: {}", userId);

        CartResponse cart = cartService.getOrCreateCart(userId);
        return ResponseEntity.ok(cart);
    }

    /**
     * Internal: Get cart by cart ID.
     */
    @GetMapping("/internal/{cartId}")
    @Operation(summary = "Get cart by ID (internal)", description = "Retrieve cart by cart ID for internal services")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    public ResponseEntity<CartResponse> getCartById(
            @Parameter(description = "Cart ID", required = true)
            @PathVariable UUID cartId) {
        log.debug("Internal get cart request: {}", cartId);
        return ResponseEntity.ok(cartService.getCartById(cartId));
    }

    /**
     * Add item to cart.
     */
    @PostMapping("/{userId}/items")
    @Operation(summary = "Add item to cart",
               description = "Add product to cart or increment quantity if already exists")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item added to cart successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "422", description = "Product not available for purchase"),
            @ApiResponse(responseCode = "503", description = "Catalog service unavailable")
    })
    public ResponseEntity<CartResponse> addItem(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId,
            @Valid @RequestBody AddItemRequest request) {

        log.info("Add item to cart request for user {}: {}", userId, request);

        CartResponse cart = cartService.addItem(userId, request);
        return ResponseEntity.ok(cart);
    }

    /**
     * Update cart item quantity.
     */
    @PutMapping("/{userId}/items/{cartItemId}")
    @Operation(summary = "Update item quantity",
               description = "Update quantity of cart item (set to 0 to remove)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item quantity updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Cart or item not found")
    })
    public ResponseEntity<CartResponse> updateItemQuantity(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId,
            @Parameter(description = "Cart item ID", required = true)
            @PathVariable UUID cartItemId,
            @Valid @RequestBody UpdateItemRequest request) {

        log.info("Update item quantity request for user {}, item {}: {}",
                userId, cartItemId, request);

        CartResponse cart = cartService.updateItemQuantity(userId, cartItemId, request);
        return ResponseEntity.ok(cart);
    }

    /**
     * Remove item from cart.
     */
    @DeleteMapping("/{userId}/items/{cartItemId}")
    @Operation(summary = "Remove item", description = "Remove item from cart")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item removed successfully"),
            @ApiResponse(responseCode = "404", description = "Cart or item not found")
    })
    public ResponseEntity<Void> removeItem(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId,
            @Parameter(description = "Cart item ID", required = true)
            @PathVariable UUID cartItemId) {

        log.info("Remove item request for user {}, item {}", userId, cartItemId);

        cartService.removeItem(userId, cartItemId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Clear all items from cart.
     */
    @DeleteMapping("/{userId}")
    @Operation(summary = "Clear cart", description = "Remove all items from cart")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cart cleared successfully"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    public ResponseEntity<Void> clearCart(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId) {

        log.info("Clear cart request for user {}", userId);

        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Checkout cart.
     */
    @PostMapping("/{userId}/checkout")
    @Operation(summary = "Checkout cart",
               description = "Mark cart as checked out and create new active cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart checked out successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot checkout empty cart"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    public ResponseEntity<CheckoutResponse> checkout(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId) {

        log.info("Checkout request for user {}", userId);

        CheckoutResponse response = cartService.checkout(userId);
        return ResponseEntity.ok(response);
    }
}
