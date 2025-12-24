package com.marketplace.inventory.domain.model;

import com.marketplace.inventory.exception.InsufficientStockException;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_items")
@EntityListeners(AuditingEntityListener.class)
public class StockItem {

    @Id
    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "available_qty", nullable = false)
    private Integer availableQty;

    @Column(name = "reserved_qty", nullable = false)
    private Integer reservedQty;

    @Column(name = "low_stock_threshold", nullable = false)
    private Integer lowStockThreshold;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected StockItem() {
        // JPA requires a no-arg constructor
    }

    public StockItem(String sku, UUID productId, Integer initialQty, Integer lowStockThreshold) {
        this.sku = sku;
        this.productId = productId;
        this.availableQty = initialQty;
        this.reservedQty = 0;
        this.lowStockThreshold = lowStockThreshold;
        this.version = 0L;
    }

    // Business logic methods

    public boolean canReserve(int quantity) {
        return availableQty >= quantity;
    }

    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new InsufficientStockException(sku, quantity, availableQty);
        }
        availableQty -= quantity;
        reservedQty += quantity;
    }

    public void releaseReservation(int quantity) {
        reservedQty -= quantity;
        availableQty += quantity;
    }

    public boolean isLowStock() {
        return availableQty <= lowStockThreshold;
    }

    public int getTotalQty() {
        return availableQty + reservedQty;
    }

    // Getters and setters

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

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
