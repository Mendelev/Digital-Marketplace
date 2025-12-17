package com.marketplace.auth.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Metrics service for tracking registration and compensating transaction metrics.
 * Uses Micrometer if available, otherwise uses simple meter registry for logging.
 */
@Component
public class RegistrationMetrics {

    private static final Logger log = LoggerFactory.getLogger(RegistrationMetrics.class);
    private final Counter registrationSuccessCounter;
    private final Counter registrationFailureCounter;
    private final Counter compensatingDeleteSuccessCounter;
    private final Counter compensatingDeleteFailureCounter;
    private final Counter orphanedUserSavedCounter;
    private final Timer registrationTimer;

    public RegistrationMetrics(@Autowired(required = false) MeterRegistry meterRegistry) {
        // Use provided registry or fallback to simple registry
        if (meterRegistry == null) {
            log.info("No MeterRegistry found, using SimpleMeterRegistry for metrics");
            meterRegistry = new SimpleMeterRegistry();
        }
        
        // Registration metrics
        this.registrationSuccessCounter = Counter.builder("registration.success")
                .description("Number of successful user registrations")
                .tag("service", "auth-service")
                .register(meterRegistry);

        this.registrationFailureCounter = Counter.builder("registration.failure")
                .description("Number of failed user registrations")
                .tag("service", "auth-service")
                .register(meterRegistry);

        // Compensating transaction metrics
        this.compensatingDeleteSuccessCounter = Counter.builder("compensating.delete.success")
                .description("Number of successful compensating deletes")
                .tag("service", "auth-service")
                .register(meterRegistry);

        this.compensatingDeleteFailureCounter = Counter.builder("compensating.delete.failure")
                .description("Number of failed compensating deletes")
                .tag("service", "auth-service")
                .register(meterRegistry);

        this.orphanedUserSavedCounter = Counter.builder("orphaned.user.saved")
                .description("Number of orphaned users saved for cleanup")
                .tag("service", "auth-service")
                .register(meterRegistry);

        // Registration timing
        this.registrationTimer = Timer.builder("registration.duration")
                .description("Registration request duration")
                .tag("service", "auth-service")
                .register(meterRegistry);
    }

    public void incrementRegistrationSuccess() {
        registrationSuccessCounter.increment();
    }

    public void incrementRegistrationFailure() {
        registrationFailureCounter.increment();
    }

    public void incrementCompensatingDeleteSuccess() {
        compensatingDeleteSuccessCounter.increment();
    }

    public void incrementCompensatingDeleteFailure() {
        compensatingDeleteFailureCounter.increment();
    }

    public void incrementOrphanedUserSaved() {
        orphanedUserSavedCounter.increment();
    }

    public Timer.Sample startRegistrationTimer() {
        return Timer.start();
    }

    public void recordRegistrationTime(Timer.Sample sample) {
        sample.stop(registrationTimer);
    }
}
