package com.marketplace.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Jackson configuration for consistent date/time serialization.
 * Configures ObjectMapper to handle OffsetDateTime properly with UTC timezone.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Register JavaTimeModule for Java 8 Date/Time API support
        mapper.registerModule(new JavaTimeModule());
        
        // Disable writing dates as timestamps (write as ISO-8601 strings instead)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Set UTC as default timezone
        mapper.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        return mapper;
    }
}
