package com.marketplace.search.consumer.mapper;

import com.marketplace.search.consumer.event.ProductCreatedEvent;
import com.marketplace.search.consumer.event.ProductUpdatedEvent;
import com.marketplace.search.document.ProductDocument;
import org.springframework.stereotype.Component;

/**
 * Maps Kafka product events to ProductDocument for indexing.
 */
@Component
public class ProductEventMapper {

    /**
     * Map ProductCreatedEvent to ProductDocument.
     */
    public ProductDocument toDocument(ProductCreatedEvent event) {
        ProductDocument document = new ProductDocument();
        document.setProductId(event.productId().toString());
        document.setName(event.name());
        document.setDescription(event.description());
        document.setBasePrice(event.basePrice());
        document.setCategoryName(event.categoryName());
        document.setSellerId(event.sellerId().toString());
        document.setStatus(event.status());
        document.setAvailableSizes(event.availableSizes());
        document.setAvailableColors(event.availableColors());
        document.setThumbnailUrl(getThumbnailUrl(event.imageUrls()));
        document.setFeatured(false); // Default to false
        document.setCreatedAt(event.createdAt());
        document.setUpdatedAt(event.updatedAt());
        return document;
    }

    /**
     * Map ProductUpdatedEvent to ProductDocument.
     */
    public ProductDocument toDocument(ProductUpdatedEvent event) {
        ProductDocument document = new ProductDocument();
        document.setProductId(event.productId().toString());
        document.setName(event.name());
        document.setDescription(event.description());
        document.setBasePrice(event.basePrice());
        document.setCategoryName(event.categoryName());
        document.setSellerId(event.sellerId().toString());
        document.setStatus(event.status());
        document.setAvailableSizes(event.availableSizes());
        document.setAvailableColors(event.availableColors());
        document.setThumbnailUrl(getThumbnailUrl(event.imageUrls()));
        document.setFeatured(false); // Default to false
        document.setCreatedAt(event.createdAt());
        document.setUpdatedAt(event.updatedAt());
        return document;
    }

    /**
     * Get thumbnail URL from image URLs (first image).
     */
    private String getThumbnailUrl(java.util.List<String> imageUrls) {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            return imageUrls.get(0);
        }
        return null;
    }
}
