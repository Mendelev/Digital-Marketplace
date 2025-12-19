package com.marketplace.search.consumer;

import com.marketplace.search.config.SearchProperties;
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
            topics = "#{@searchProperties.kafka().topic()}",
            groupId = "#{@kafkaProperties.consumer.groupId}",
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
}
