package com.marketplace.catalog.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "product_events")
public class ProductEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;
    
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;
    
    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;
    
    @Column(name = "published_at", nullable = false, updatable = false)
    private OffsetDateTime publishedAt;
    
    @PrePersist
    protected void onCreate() {
        publishedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
    
    public ProductEvent() {
    }
    
    public ProductEvent(Long id, UUID eventId, UUID productId, String eventType, Long sequenceNumber, Map<String, Object> payload, OffsetDateTime publishedAt) {
        this.id = id;
        this.eventId = eventId;
        this.productId = productId;
        this.eventType = eventType;
        this.sequenceNumber = sequenceNumber;
        this.payload = payload;
        this.publishedAt = publishedAt;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public UUID getEventId() {
        return eventId;
    }
    
    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }
    
    public UUID getProductId() {
        return productId;
    }
    
    public void setProductId(UUID productId) {
        this.productId = productId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public Long getSequenceNumber() {
        return sequenceNumber;
    }
    
    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
    
    public Map<String, Object> getPayload() {
        return payload;
    }
    
    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
    
    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }
    
    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
