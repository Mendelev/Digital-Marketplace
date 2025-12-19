package com.marketplace.catalog.domain.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "product_price_history")
public class ProductPriceHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    
    @Column(name = "old_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal oldPrice;
    
    @Column(name = "new_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal newPrice;
    
    @Column(name = "changed_at", nullable = false, updatable = false)
    private OffsetDateTime changedAt;
    
    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;
    
    @PrePersist
    protected void onCreate() {
        changedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
    
    public ProductPriceHistory() {
    }
    
    public ProductPriceHistory(Long id, UUID productId, BigDecimal oldPrice, BigDecimal newPrice, OffsetDateTime changedAt, UUID changedBy) {
        this.id = id;
        this.productId = productId;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public UUID getProductId() {
        return productId;
    }
    
    public void setProductId(UUID productId) {
        this.productId = productId;
    }
    
    public BigDecimal getOldPrice() {
        return oldPrice;
    }
    
    public void setOldPrice(BigDecimal oldPrice) {
        this.oldPrice = oldPrice;
    }
    
    public BigDecimal getNewPrice() {
        return newPrice;
    }
    
    public void setNewPrice(BigDecimal newPrice) {
        this.newPrice = newPrice;
    }
    
    public OffsetDateTime getChangedAt() {
        return changedAt;
    }
    
    public void setChangedAt(OffsetDateTime changedAt) {
        this.changedAt = changedAt;
    }
    
    public UUID getChangedBy() {
        return changedBy;
    }
    
    public void setChangedBy(UUID changedBy) {
        this.changedBy = changedBy;
    }
}
