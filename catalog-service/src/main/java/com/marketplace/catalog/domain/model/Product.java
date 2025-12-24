package com.marketplace.catalog.domain.model;

import com.marketplace.catalog.domain.enums.ProductStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;
    
    @Column(nullable = false, length = 500)
    private String name;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "available_sizes", columnDefinition = "text[]")
    private String[] availableSizes;
    
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "available_colors", columnDefinition = "text[]")
    private String[] availableColors;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "stock_per_variant", columnDefinition = "jsonb")
    private Map<String, Integer> stockPerVariant;
    
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "image_urls", nullable = false, columnDefinition = "text[]")
    private String[] imageUrls;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ProductStatus status = ProductStatus.ACTIVE;
    
    @Column(name = "is_featured", nullable = false)
    private boolean featured = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    
    public Product() {}
    
    public Product(UUID id, UUID sellerId, String name, String description, BigDecimal basePrice, Category category,
                   String[] availableSizes, String[] availableColors, Map<String, Integer> stockPerVariant,
                   String[] imageUrls, ProductStatus status, boolean featured, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.sellerId = sellerId;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.category = category;
        this.availableSizes = availableSizes;
        this.availableColors = availableColors;
        this.stockPerVariant = stockPerVariant;
        this.imageUrls = imageUrls;
        this.status = status;
        this.featured = featured;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        updatedAt = createdAt;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
    
    public Integer getStockForVariant(String size, String color) {
        if (stockPerVariant == null) {
            return 0;
        }
        String key = size + "-" + color;
        return stockPerVariant.getOrDefault(key, 0);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getSellerId() { return sellerId; }
    public void setSellerId(UUID sellerId) { this.sellerId = sellerId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    
    public String[] getAvailableSizes() { return availableSizes; }
    public void setAvailableSizes(String[] availableSizes) { this.availableSizes = availableSizes; }
    
    public String[] getAvailableColors() { return availableColors; }
    public void setAvailableColors(String[] availableColors) { this.availableColors = availableColors; }
    
    public Map<String, Integer> getStockPerVariant() { return stockPerVariant; }
    public void setStockPerVariant(Map<String, Integer> stockPerVariant) { this.stockPerVariant = stockPerVariant; }
    
    public String[] getImageUrls() { return imageUrls; }
    public void setImageUrls(String[] imageUrls) { this.imageUrls = imageUrls; }
    
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }
    
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
