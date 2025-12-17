package com.marketplace.user.config;

import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

/**
 * Custom DateTimeProvider that returns OffsetDateTime for JPA auditing.
 */
@Component("dateTimeProvider")
public class OffsetDateTimeProvider implements DateTimeProvider {

    @Override
    public Optional<TemporalAccessor> getNow() {
        return Optional.of(OffsetDateTime.now());
    }
}
