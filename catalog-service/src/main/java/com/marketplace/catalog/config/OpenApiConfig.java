package com.marketplace.catalog.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for Catalog Service API documentation.
 * Provides interactive API documentation with Basic Auth (automatically converted to JWT).
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Catalog Service API",
                version = "1.0.0",
                description = "Product catalog management service for Digital Marketplace. " +
                        "Manages products, categories, pricing, inventory, and publishes events to Kafka. " +
                        "Supports multi-seller product management with role-based authorization. " +
                        "**Authentication:** Click 'Authorize' and enter your email as username and password. " +
                        "JWT token will be obtained automatically.",
                contact = @Contact(
                        name = "Digital Marketplace Team",
                        email = "support@marketplace.com"
                )
        ),
        servers = {
                @Server(
                        description = "Local Development",
                        url = "http://localhost:8082"
                ),
                @Server(
                        description = "Docker Container",
                        url = "http://catalog-service:8082"
                )
        },
        security = {
                @SecurityRequirement(name = "basicAuth")
        }
)
@SecurityScheme(
        name = "basicAuth",
        description = "Enter your email as username and password. JWT token will be obtained automatically for all requests.",
        type = SecuritySchemeType.HTTP,
        scheme = "basic"
)
public class OpenApiConfig {
}
