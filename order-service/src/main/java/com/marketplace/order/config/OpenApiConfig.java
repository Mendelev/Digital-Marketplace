package com.marketplace.order.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for Order Service.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Order Service API",
        version = "1.0.0",
        description = """
            Order management and orchestration service for Digital Marketplace.

            Features:
            - Create orders from shopping carts
            - Order lifecycle management (pending → confirmed → shipped → delivered)
            - Payment authorization and capture
            - Inventory reservation with TTL
            - Order cancellation workflow
            - Event publishing to Kafka
            """,
        contact = @Contact(
            name = "Digital Marketplace Team",
            email = "dev@digitalmarketplace.com"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8086", description = "Local development server"),
        @Server(url = "http://order-service:8086", description = "Docker container")
    },
    security = {
        @SecurityRequirement(name = "basicAuth")
    }
)
@SecurityScheme(
    name = "basicAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "basic",
    description = "HTTP Basic Authentication (will be converted to JWT token in future)"
)
public class OpenApiConfig {
}
