package com.marketplace.search.consumer;

import com.marketplace.search.consumer.event.CatalogProductEvent;
import com.marketplace.search.consumer.event.ProductCreatedEvent;
import com.marketplace.search.consumer.event.ProductDeletedEvent;
import com.marketplace.search.consumer.event.ProductUpdatedEvent;
import com.marketplace.search.consumer.mapper.ProductEventMapper;
import com.marketplace.search.document.ProductDocument;
import com.marketplace.search.service.IndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Kafka consumer for product events.
 * Listens to product-events topic and updates Elasticsearch index.
 */
@Component
public class ProductEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventConsumer.class);
    private static final String CORRELATION_ID_KEY = "correlationId";

    private final IndexingService indexingService;
    private final ProductEventMapper productEventMapper;

    public ProductEventConsumer(IndexingService indexingService,
                               ProductEventMapper productEventMapper) {
        this.indexingService = indexingService;
        this.productEventMapper = productEventMapper;
    }

    @KafkaListener(
            topics = "${search.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeProductEvent(@Payload Object event,
                                   @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                                   @Header(value = "X-Correlation-ID", required = false) String correlationId,
                                   Acknowledgment acknowledgment) {
        try {
            // Set correlation ID for tracing
            if (correlationId != null) {
                MDC.put(CORRELATION_ID_KEY, correlationId);
            }

            log.info("Received product event: type={}, key={}", event.getClass().getSimpleName(), key);

            // Route event to appropriate handler
            if (event instanceof ProductCreatedEvent createdEvent) {
                handleProductCreated(createdEvent);
            } else if (event instanceof ProductUpdatedEvent updatedEvent) {
                handleProductUpdated(updatedEvent);
            } else if (event instanceof ProductDeletedEvent deletedEvent) {
                handleProductDeleted(deletedEvent);
            } else if (event instanceof CatalogProductEvent catalogEvent) {
                handleCatalogEvent(catalogEvent);
            } else {
                log.warn("Unknown event type: {}", event.getClass().getName());
            }

            // Acknowledge only after successful processing
            acknowledgment.acknowledge();
            log.debug("Event processed and acknowledged successfully");

        } catch (Exception e) {
            log.error("Error processing product event", e);
            // Do not acknowledge - will retry
            throw new RuntimeException("Failed to process product event", e);
        } finally {
            MDC.remove(CORRELATION_ID_KEY);
        }
    }

    /**
     * Handle ProductCreatedEvent - index new product.
     */
    private void handleProductCreated(ProductCreatedEvent event) {
        log.info("Handling ProductCreatedEvent for productId: {}", event.productId());

        ProductDocument document = productEventMapper.toDocument(event);
        indexingService.indexProduct(document);

        log.info("Product created and indexed: {}", event.productId());
    }

    /**
     * Handle ProductUpdatedEvent - update existing product.
     */
    private void handleProductUpdated(ProductUpdatedEvent event) {
        log.info("Handling ProductUpdatedEvent for productId: {}", event.productId());

        ProductDocument document = productEventMapper.toDocument(event);
        indexingService.updateProduct(document);

        log.info("Product updated in index: {}", event.productId());
    }

    /**
     * Handle ProductDeletedEvent - remove product from index.
     */
    private void handleProductDeleted(ProductDeletedEvent event) {
        log.info("Handling ProductDeletedEvent for productId: {}", event.productId());

        indexingService.deleteProduct(event.productId().toString());

        log.info("Product deleted from index: {}", event.productId());
    }

    private void handleCatalogEvent(CatalogProductEvent event) {
        String eventType = event.eventType();
        log.info("Handling Catalog event type={} for productId: {}", eventType, event.productId());

        if (eventType == null || eventType.isBlank()) {
            log.warn("Catalog event missing eventType for productId: {}", event.productId());
            return;
        }

        switch (eventType) {
            case "ProductCreated" -> handleCatalogCreated(event);
            case "ProductUpdated" -> handleCatalogUpdated(event);
            case "ProductDeleted" -> handleCatalogDeleted(event);
            default -> log.warn("Unsupported catalog eventType: {}", eventType);
        }
    }

    private void handleCatalogCreated(CatalogProductEvent event) {
        ProductDocument document = toDocument(event.payload());
        if (document == null) {
            log.warn("Catalog ProductCreated event missing payload for productId: {}", event.productId());
            return;
        }
        indexingService.indexProduct(document);
    }

    private void handleCatalogUpdated(CatalogProductEvent event) {
        ProductDocument document = toDocument(event.payload());
        if (document == null) {
            log.warn("Catalog ProductUpdated event missing payload for productId: {}", event.productId());
            return;
        }
        indexingService.updateProduct(document);
    }

    private void handleCatalogDeleted(CatalogProductEvent event) {
        if (event.payload() != null && event.payload().get("productId") != null) {
            indexingService.deleteProduct(event.payload().get("productId").toString());
            return;
        }
        if (event.productId() != null) {
            indexingService.deleteProduct(event.productId().toString());
        } else {
            log.warn("Catalog ProductDeleted event missing productId");
        }
    }

    private ProductDocument toDocument(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }

        ProductDocument document = new ProductDocument();
        document.setProductId(asString(payload.get("productId")));
        document.setSellerId(asString(payload.get("sellerId")));
        document.setName(asString(payload.get("name")));
        document.setDescription(asString(payload.get("description")));
        document.setCategoryName(asString(payload.get("categoryName")));
        document.setStatus(asString(payload.get("status")));
        document.setBasePrice(asBigDecimal(payload.get("basePrice")));
        document.setAvailableSizes(asStringList(payload.get("availableSizes")));
        document.setAvailableColors(asStringList(payload.get("availableColors")));
        document.setThumbnailUrl(extractThumbnailUrl(payload.get("imageUrls")));
        document.setFeatured(asBoolean(payload.get("featured")));
        document.setCreatedAt(asInstant(payload.get("createdAt")));
        document.setUpdatedAt(asInstant(payload.get("updatedAt")));

        return document;
    }

    private String extractThumbnailUrl(Object imageUrlsValue) {
        List<String> urls = asStringList(imageUrlsValue);
        if (urls != null && !urls.isEmpty()) {
            return urls.get(0);
        }
        return null;
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        return new BigDecimal(value.toString());
    }

    private Boolean asBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value.toString());
    }

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            return Arrays.stream(array).map(Object::toString).toList();
        }
        return List.of(value.toString());
    }

    private Instant asInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        return OffsetDateTime.parse(value.toString()).toInstant();
    }
}
