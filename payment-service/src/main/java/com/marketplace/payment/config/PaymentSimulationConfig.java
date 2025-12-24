package com.marketplace.payment.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "payment-simulation")
@Validated
public record PaymentSimulationConfig(
    @DecimalMin(value = "0.0", message = "Authorization success rate must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Authorization success rate must be between 0 and 1")
    double authorizationSuccessRate,

    @DecimalMin(value = "0.0", message = "Capture success rate must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Capture success rate must be between 0 and 1")
    double captureSuccessRate,

    @DecimalMin(value = "0.0", message = "Refund success rate must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Refund success rate must be between 0 and 1")
    double refundSuccessRate,

    @DecimalMin(value = "0.0", message = "Void success rate must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Void success rate must be between 0 and 1")
    double voidSuccessRate,

    @Min(value = 0, message = "Simulation delay must be non-negative")
    int simulationDelayMs
) {}
