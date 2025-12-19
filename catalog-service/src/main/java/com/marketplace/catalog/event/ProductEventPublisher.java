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
        publishEvent(
            "ProductCreated",
            product.getId(),
            Map.of(
                "productId", product.getId().toString(),
                "sellerId", product.getSellerId().toString(),
                "name", product.getName(),
                "basePrice", product.getBasePrice().toString(),
                "categoryId", product.getCategory().getId(),
                "categoryName", product.getCategory().getName(),
                "status", product.getStatus().name(),
                "createdAt", product.getCreatedAt().toString()
            )
        );
    }
    
    public void publishProductUpdated(Product product) {
        publishEvent(
            "ProductUpdated",
            product.getId(),
            Map.of(
                "productId", product.getId().toString(),
                "name", product.getName(),
                "basePrice", product.getBasePrice().toString(),
                "status", product.getStatus().name(),
                "updatedAt", product.getUpdatedAt().toString()
            )
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
        
        boolean sent = streamBridge.send("product-events-out-0", message);
        if (sent) {
            log.info("Published {} event to Kafka for product: {}", eventType, productId);
        } else {
            log.error("Failed to publish {} event to Kafka for product: {}", eventType, productId);
        }
    }
}
