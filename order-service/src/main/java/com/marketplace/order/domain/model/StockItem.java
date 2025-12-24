package com.marketplace.order.domain.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Stock item entity for mock inventory service.
 */
@Entity
@Table(name = "stock_items")
@EntityListeners(AuditingEntityListener.class)
public class StockItem {

    @Id
    @Column(length = 100)
    private String sku;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "available_qty", nullable = false)
    private Integer availableQty = 0;

    @Column(name = "reserved_qty", nullable = false)
    private Integer reservedQty = 0;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Constructors
    public StockItem() {
    }

    public StockItem(String sku, UUID productId, Integer availableQty) {
        this.sku = sku;
        this.productId = productId;
        this.availableQty = availableQty;
        this.reservedQty = 0;
    }

    // Getters and Setters
    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public Integer getAvailableQty() {
        return availableQty;
    }

    public void setAvailableQty(Integer availableQty) {
        this.availableQty = availableQty;
    }

    public Integer getReservedQty() {
        return reservedQty;
    }

    public void setReservedQty(Integer reservedQty) {
        this.reservedQty = reservedQty;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
