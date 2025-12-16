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
}
