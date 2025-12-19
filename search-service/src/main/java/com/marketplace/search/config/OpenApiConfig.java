package com.marketplace.search.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for Search Service.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Search Service API",
        version = "1.0.0",
        description = """
            Product search and discovery API for Digital Marketplace.

            Features:
            - Full-text search across product catalog
            - Advanced filtering (category, price range, seller, availability)
            - Sort by relevance, price, or recency
            - Paginated results
            - Autocomplete suggestions
            """,
        contact = @Contact(
            name = "Digital Marketplace Team",
            email = "dev@digitalmarketplace.com"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8085", description = "Local development server"),
        @Server(url = "http://search-service:8085", description = "Docker container")
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
