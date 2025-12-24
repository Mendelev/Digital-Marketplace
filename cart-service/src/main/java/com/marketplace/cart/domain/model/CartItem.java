package com.marketplace.cart.domain.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * CartItem entity representing items in a shopping cart.
 */
@Entity
@Table(name = "cart_item")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cart_item_id")
    private UUID cartItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(name = "title_snapshot", nullable = false, length = 500)
    private String titleSnapshot;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Integer quantity;

    protected CartItem() {
    }

    public CartItem(UUID productId, String sku, String titleSnapshot,
                    BigDecimal unitPriceSnapshot, String currency, Integer quantity) {
        this.productId = productId;
        this.sku = sku;
        this.titleSnapshot = titleSnapshot;
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.currency = currency;
        this.quantity = quantity;
    }

    // Business method to compute item subtotal
    public BigDecimal getSubtotal() {
        return unitPriceSnapshot.multiply(BigDecimal.valueOf(quantity));
    }

    // Getters and Setters
    public UUID getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(UUID cartItemId) {
        this.cartItemId = cartItemId;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getTitleSnapshot() {
        return titleSnapshot;
    }

    public void setTitleSnapshot(String titleSnapshot) {
        this.titleSnapshot = titleSnapshot;
    }

    public BigDecimal getUnitPriceSnapshot() {
        return unitPriceSnapshot;
    }

    public void setUnitPriceSnapshot(BigDecimal unitPriceSnapshot) {
        this.unitPriceSnapshot = unitPriceSnapshot;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
