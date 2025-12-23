package com.marketplace.catalog.event;

import com.marketplace.catalog.domain.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.marketplace.catalog.domain.model.ProductEvent;
import com.marketplace.catalog.domain.repository.ProductEventRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProductEventPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(ProductEventPublisher.class);
    private final ProductEventRepository eventRepository;
    private final StreamBridge streamBridge;
    private final AtomicLong sequenceGenerator = new AtomicLong(0);
    
    public ProductEventPublisher(ProductEventRepository eventRepository, StreamBridge streamBridge) {
        this.eventRepository = eventRepository;
        this.streamBridge = streamBridge;
    }
    
    @PostConstruct
    public void initSequence() {
        Long maxSequence = eventRepository.findMaxSequenceNumber();
        sequenceGenerator.set(maxSequence != null ? maxSequence : 0);
        log.info("Initialized event sequence number at: {}", sequenceGenerator.get());
    }
    
    public void publishProductCreated(Product product) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", product.getId().toString());
        payload.put("sellerId", product.getSellerId().toString());
        payload.put("name", product.getName());
        payload.put("description", product.getDescription());
        payload.put("basePrice", product.getBasePrice().toString());
        payload.put("categoryId", product.getCategory().getId());
        payload.put("categoryName", product.getCategory().getName());
        payload.put("status", product.getStatus().name());
        payload.put("availableSizes", product.getAvailableSizes());
        payload.put("availableColors", product.getAvailableColors());
        payload.put("imageUrls", product.getImageUrls());
        payload.put("featured", product.isFeatured());
        payload.put("createdAt", product.getCreatedAt().toString());
        payload.put("updatedAt", product.getUpdatedAt().toString());

        publishEvent(
            "ProductCreated",
            product.getId(),
            payload
        );
    }
    
    public void publishProductUpdated(Product product) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", product.getId().toString());
        payload.put("sellerId", product.getSellerId().toString());
        payload.put("name", product.getName());
        payload.put("description", product.getDescription());
        payload.put("basePrice", product.getBasePrice().toString());
        payload.put("categoryId", product.getCategory().getId());
        payload.put("categoryName", product.getCategory().getName());
        payload.put("status", product.getStatus().name());
        payload.put("availableSizes", product.getAvailableSizes());
        payload.put("availableColors", product.getAvailableColors());
        payload.put("imageUrls", product.getImageUrls());
        payload.put("featured", product.isFeatured());
        payload.put("createdAt", product.getCreatedAt().toString());
        payload.put("updatedAt", product.getUpdatedAt().toString());

        publishEvent(
            "ProductUpdated",
            product.getId(),
            payload
        );
    }
    
    public void publishProductDeleted(UUID productId) {
        publishEvent(
            "ProductDeleted",
            productId,
            Map.of(
                "productId", productId.toString()
            )
        );
    }
    
    private void publishEvent(String eventType, UUID productId, Map<String, Object> payload) {
        UUID eventId = UUID.randomUUID();
        long sequenceNumber = sequenceGenerator.incrementAndGet();
        
        // Check idempotency (shouldn't happen, but safety check)
        if (eventRepository.existsByEventId(eventId)) {
            log.warn("Event {} already exists, skipping publication", eventId);
            return;
        }
        
        ProductEvent event = new ProductEvent();
        event.setEventId(eventId);
        event.setProductId(productId);
        event.setEventType(eventType);
        event.setSequenceNumber(sequenceNumber);
        event.setPayload(payload);
        
        eventRepository.save(event);
        log.info("Saved {} event (id: {}, sequence: {}) for product: {}", 
            eventType, eventId, sequenceNumber, productId);
        
        // Publish to Kafka with ordering key (productId)
        Message<ProductEvent> message = MessageBuilder
            .withPayload(event)
            .setHeader(KafkaHeaders.KEY, productId.toString())  // Ensures ordering per product
            .setHeader("event-id", eventId.toString())
            .setHeader("sequence-number", sequenceNumber)
            .build();
        
        try {
            boolean sent = streamBridge.send("product-events-out-0", message);
            if (sent) {
                log.info("Published {} event to Kafka for product: {}", eventType, productId);
            } else {
                log.warn("Failed to publish {} event to Kafka for product: {}", eventType, productId);
            }
        } catch (Exception e) {
            // Allow product operations to proceed even if Kafka is unavailable.
            log.warn("Skipping Kafka publish for {} event (product: {}): {}", eventType, productId, e.getMessage());
        }
    }
}
