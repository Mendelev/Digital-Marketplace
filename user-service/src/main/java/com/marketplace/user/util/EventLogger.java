package com.marketplace.user.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Utility for publishing structured domain events as logs.
 */
@Component
public class EventLogger {

    private static final Logger log = LoggerFactory.getLogger(EventLogger.class);

    /**
     * Log a domain event with changed fields only.
     *
     * @param eventType     Type of event (UserCreated, UserUpdated, etc.)
     * @param aggregateType Type of aggregate (USER, ADDRESS, PREFERENCES)
     * @param aggregateId   ID of the aggregate
     * @param changedFields Map of changed field names to new values
     */
    public void logEvent(String eventType, String aggregateType, UUID aggregateId, Map<String, Object> changedFields) {
        try {
            MDC.put("eventType", eventType);
            MDC.put("aggregateType", aggregateType);
            MDC.put("aggregateId", aggregateId.toString());

            log.info("Domain event: {} on {} {}, changes: {}",
                    eventType, aggregateType, aggregateId, changedFields);

        } finally {
            MDC.remove("eventType");
            MDC.remove("aggregateType");
            MDC.remove("aggregateId");
        }
    }

    /**
     * Log a domain event without changed fields (for creation events).
     *
     * @param eventType     Type of event
     * @param aggregateType Type of aggregate
     * @param aggregateId   ID of the aggregate
     * @param snapshot      Full object snapshot
     */
    public void logEvent(String eventType, String aggregateType, UUID aggregateId, Object snapshot) {
        try {
            MDC.put("eventType", eventType);
            MDC.put("aggregateType", aggregateType);
            MDC.put("aggregateId", aggregateId.toString());

            log.info("Domain event: {} on {} {}, snapshot: {}",
                    eventType, aggregateType, aggregateId, snapshot);

        } finally {
            MDC.remove("eventType");
            MDC.remove("aggregateType");
            MDC.remove("aggregateId");
        }
    }

    /**
     * Log address created event.
     */
    public void logAddressCreated(com.marketplace.user.domain.model.Address address) {
        Map<String, Object> snapshot = Map.of(
                "label", address.getLabel(),
                "country", address.getCountry(),
                "city", address.getCity()
        );
        logEvent("AddressCreated", "ADDRESS", address.getAddressId(), snapshot);
    }

    /**
     * Log address updated event with changed fields.
     */
    public void logAddressUpdated(com.marketplace.user.domain.model.Address oldAddress, 
                                  com.marketplace.user.domain.model.Address newAddress) {
        Map<String, Object> changes = new java.util.HashMap<>();
        
        if (!oldAddress.getLabel().equals(newAddress.getLabel())) {
            changes.put("label", newAddress.getLabel());
        }
        if (!oldAddress.getCountry().equals(newAddress.getCountry())) {
            changes.put("country", newAddress.getCountry());
        }
        if (!oldAddress.getState().equals(newAddress.getState())) {
            changes.put("state", newAddress.getState());
        }
        if (!oldAddress.getCity().equals(newAddress.getCity())) {
            changes.put("city", newAddress.getCity());
        }
        if (!oldAddress.getZip().equals(newAddress.getZip())) {
            changes.put("zip", newAddress.getZip());
        }
        if (!oldAddress.getStreet().equals(newAddress.getStreet())) {
            changes.put("street", newAddress.getStreet());
        }
        if (!oldAddress.getNumber().equals(newAddress.getNumber())) {
            changes.put("number", newAddress.getNumber());
        }
        
        if (!changes.isEmpty()) {
            logEvent("AddressUpdated", "ADDRESS", newAddress.getAddressId(), changes);
        }
    }

    /**
     * Log address deleted event.
     */
    public void logAddressDeleted(com.marketplace.user.domain.model.Address address) {
        Map<String, Object> snapshot = Map.of(
                "label", address.getLabel()
        );
        logEvent("AddressDeleted", "ADDRESS", address.getAddressId(), snapshot);
    }

    /**
     * Log default address changed event.
     */
    public void logDefaultAddressChanged(com.marketplace.user.domain.model.Address address, String type) {
        Map<String, Object> data = Map.of(
                "defaultType", type,
                "label", address.getLabel()
        );
        logEvent("DefaultAddressChanged", "ADDRESS", address.getAddressId(), data);
    }
}
