# Architecture Decision Records (ADR)

This document captures key architectural and technical decisions made for the Digital Marketplace platform.

---

## ADR-001: Technology Stack

**Date**: 2025-12-16  
**Status**: Accepted  
**Context**: Need to select core technology stack for microservices implementation.

**Decision**:
- **Language**: Java 21
- **Framework**: Spring Boot 3.x
- **Build Tool**: Maven
- **Runtime**: JVM (OpenJDK 21+)

**Rationale**:
- Java 21 provides modern language features (records, pattern matching, virtual threads)
- Spring Boot is industry-standard with excellent ecosystem and tooling
- Maven provides reliable dependency management and build lifecycle
- Large community support and extensive documentation

**Consequences**:
- Team must be proficient in Java and Spring ecosystem
- Requires JVM runtime in deployment environments
- Benefits from mature tooling and IDE support

---

## ADR-002: Database Strategy

**Date**: 2025-12-16  
**Status**: Accepted  
**Context**: Need to define database architecture for microservices.

**Decision**:
- Each microservice owns its dedicated PostgreSQL database
- No shared databases between services
- Database-per-service pattern

**Rationale**:
- Ensures loose coupling and service autonomy
- Allows independent scaling and schema evolution
- PostgreSQL provides excellent UUID support, ACID guarantees, and JSON capabilities
- Prevents tight coupling through shared data access

**Consequences**:
- Data consistency requires distributed transaction patterns (saga, event sourcing)
- Increased operational complexity (multiple databases to manage)
- No direct database joins across services
- Requires careful API design for cross-service queries

---

## ADR-003: Inter-Service Communication

**Date**: 2025-12-16  
**Status**: Accepted  
**Context**: Need to define how microservices communicate with each other.

**Decision**:
- **Synchronous**: REST APIs with OpenAPI/Swagger contracts
- **Configuration**: Direct URL configuration (no service discovery for MVP)
- **Protocol**: HTTP/HTTPS with JSON payloads
- **Future**: Event-driven communication to be added in later phases

**Rationale**:
- REST APIs are well-understood and have excellent tooling
- OpenAPI specs provide contract-first development and documentation
- Direct URLs simplify initial implementation and debugging
- Can evolve to service discovery (Eureka, Consul) when needed

**Consequences**:
- Services must handle downstream failures gracefully (circuit breakers)
- Configuration management for service URLs required
- No automatic service discovery or load balancing
- Eventual consistency will be handled via events in future phases

---

## ADR-004: API Contract Management

**Date**: 2025-12-16  
**Status**: Accepted  
**Context**: Need standardized API documentation and contract definitions.

**Decision**:
- Use OpenAPI 3.0 specification for all REST APIs
- Springdoc OpenAPI for automatic Swagger UI generation
- Shared schema definitions in `common/api-schemas/` directory
- API-first approach with YAML specifications

**Rationale**:
- OpenAPI is industry standard for REST API documentation
- Enables contract-first development and testing
- Springdoc integrates seamlessly with Spring Boot
- Shared schemas ensure consistency across services

**Consequences**:
- Developers must maintain OpenAPI specs alongside code
- Requires tooling for spec validation and generation
- Benefits from automatic documentation and client generation

---

## ADR-005: Authentication & Authorization

**Date**: 2025-12-16  
**Status**: Accepted  
**Context**: Need secure authentication mechanism for microservices.

**Decision**:
- JWT (JSON Web Tokens) with RS256 algorithm
- Auth Service issues and validates tokens
- Public key distribution via dedicated endpoint (`/api/v1/auth/public-key`)
- Token types: short-lived access tokens (15 min), long-lived refresh tokens (7 days)
- Library: `jjwt` (io.jsonwebtoken) version 0.12.x

**Rationale**:
- RS256 (asymmetric) allows services to verify tokens without shared secrets
- Public key can be safely distributed to all services
- JWT is stateless and scalable (no session storage)
- Industry-standard approach with excellent library support
- Refresh tokens minimize security risk of long-lived access tokens

**Consequences**:
- Services must fetch and cache public key for validation
- Token revocation requires additional mechanism (refresh token blacklist)
- Private key must be secured and never exposed
- Clock synchronization important for token expiration

---

## ADR-006: Error Handling & Response Format

**Date**: 2025-12-16  
**Status**: Accepted  
**Context**: Need consistent error response format across all services.

**Decision**:
- Standardized `ErrorResponse` schema with fields:
  - `timestamp`: ISO 8601 timestamp
  - `status`: HTTP status code
  - `error`: Error type/category
  - `message`: Human-readable description
  - `path`: Request path
  - `correlationId`: Tracing identifier
- Global exception handlers in each service
- Correlation ID propagated across all service calls

**Rationale**:
- Consistent error format improves client experience
- Correlation IDs enable distributed tracing and debugging
- Structured errors facilitate monitoring and alerting

**Consequences**:
- All services must implement global exception handlers
- Requires discipline to map domain exceptions to HTTP status codes
- Benefits debugging and production troubleshooting significantly

---

## ADR-007: Observability & Logging

**Date**: 2025-12-16  
**Status**: Accepted  
**Context**: Need comprehensive logging for debugging and monitoring.

**Decision**:
- Structured JSON logging using Logstash Logback Encoder
- MDC (Mapped Diagnostic Context) for contextual fields:
  - `correlationId`: Request tracking
  - `userId`: User context (when available)
  - `operation`: Business operation being performed
- Aspect-oriented logging for service layer methods
- Log method entry/exit, execution time, and sanitized arguments

**Rationale**:
- JSON logs are machine-readable and integrate with log aggregation tools
- MDC provides request context without passing parameters
- Structured logs facilitate searching and analysis
- AOP keeps logging concerns separate from business logic

**Consequences**:
- Log files are larger but more useful
- Requires log aggregation solution for production (ELK, Splunk, etc.)
- Sensitive data must be explicitly sanitized in logs

---

## ADR-008: Request Tracing

**Date**: 2025-12-16  
**Status**: Accepted  
**Context**: Need to trace requests across microservices.

**Decision**:
- Use `X-Correlation-ID` HTTP header
- Generate UUID if not present in incoming request
- Propagate to all downstream service calls
- Store in MDC for logging
- Return in response headers

**Rationale**:
- Industry-standard header name
- UUID provides unique global identifiers
- Enables end-to-end request tracing in distributed system
- Simple to implement with servlet filters

**Consequences**:
- All services must implement correlation ID filter
- HTTP clients must be configured to propagate header
- Benefits troubleshooting and performance analysis

---

## ADR-009: Service Stubs for Development

**Date**: 2025-12-16  
**Status**: Accepted  
**Context**: Need to develop services independently before dependencies are ready.

**Decision**:
- Use Mockoon containerized for service mocking
- Mock configurations stored in service repositories
- Each mock provides sample endpoints and responses
- Mocks run in Docker Compose alongside service

**Rationale**:
- Mockoon is lightweight and easy to configure
- Containerized approach ensures consistency across developers
- Enables parallel development of services
- Configuration as code (JSON files) in version control

**Consequences**:
- Mock responses must be kept in sync with real API contracts
- Developers must remember to test against real services eventually
- Simplifies initial development and testing

---

## ADR-010: Resilience Patterns

**Date**: 2025-12-16  
**Status**: Accepted  
**Context**: Services must handle downstream failures gracefully.

**Decision**:
- Use Resilience4j for resilience patterns
- Circuit breaker for external service calls
- Fallback methods for degraded functionality
- Configurable timeouts and retry policies

**Rationale**:
- Resilience4j is lightweight and Spring-native
- Circuit breakers prevent cascading failures
- Explicit fallback handling improves system stability
- Industry best practice for microservices

**Consequences**:
- Developers must design fallback behavior
- Requires monitoring of circuit breaker states
- Additional configuration complexity
- Improved system resilience and user experience

---

## ADR-011: Testing Strategy

**Date**: 2025-12-16  
**Status**: Accepted  
**Context**: Need testing approach for microservices.

**Decision**:
- **Unit Tests**: JUnit 5 + Mockito for business logic
- **Focus**: Service layer and utility classes
- **Mocking**: Mock external dependencies (repositories, HTTP clients)
- **Deferred**: Integration tests with Testcontainers for later phases

**Rationale**:
- Unit tests provide fast feedback and high coverage
- Mockito integrates well with Spring and JUnit 5
- Can iterate quickly without infrastructure dependencies
- Integration tests add value but slow down development cycle

**Consequences**:
- Must ensure mocks accurately represent real behavior
- Integration issues may surface later in development
- Fast test execution enables TDD practices

---

## ADR-012: Monorepo Structure

**Date**: 2025-12-16  
**Status**: Accepted  
**Context**: Need to organize multiple microservices in repository.

**Decision**:
- Single Git repository (monorepo)
- Services in separate top-level folders (`auth-service/`, `user-service/`, etc.)
- Shared artifacts in `common/` directory
- Each service is independently buildable and deployable

**Rationale**:
- Simplified dependency management and versioning
- Easier refactoring across service boundaries
- Atomic commits across services
- Single source of truth for entire platform

**Consequences**:
- Repository size grows with number of services
- CI/CD must handle selective builds
- Requires discipline to maintain service boundaries
- Benefits code reuse and consistency

---

## ADR-013: Password Security

**Date**: 2025-12-16  
**Status**: Accepted  
**Context**: Need secure password storage and validation.

**Decision**:
- BCrypt password hashing with strength factor 12
- No password complexity requirements (sample project)
- Failed login tracking with account lockout after 5 attempts
- Password reset via time-limited tokens (1-hour TTL)

**Rationale**:
- BCrypt is industry-standard for password hashing
- Strength 12 balances security and performance
- Account lockout prevents brute force attacks
- Time-limited tokens limit exposure window

**Consequences**:
- Password hashing is computationally expensive (by design)
- Locked accounts require manual or automated unlock process
- Reset tokens must be securely generated and stored

---

## ADR-014: Token Management

**Date**: 2025-12-16  
**Status**: Accepted  
**Context**: Need to manage token lifecycle and cleanup.

**Decision**:
- Store refresh tokens in database (hashed)
- Manual cleanup endpoint (`DELETE /api/v1/auth/tokens/cleanup`)
- No automated scheduled jobs for MVP
- Token rotation on refresh (invalidate old token)

**Rationale**:
- Database storage enables revocation
- Manual cleanup sufficient for sample project
- Token rotation limits exposure of compromised tokens
- Avoids complexity of scheduled job infrastructure

**Consequences**:
- Database size grows with token accumulation
- Manual cleanup required periodically
- Can evolve to scheduled jobs if needed

---

## ADR-015: API Gateway Strategy

**Date**: 2025-12-16  
**Status**: Deferred  
**Context**: Need to consider API Gateway for routing and security.

**Decision**:
- Defer API Gateway implementation to later phases
- Services exposed directly during MVP
- Focus on individual service development first

**Rationale**:
- Simplifies initial development and debugging
- Gateway adds complexity and operational overhead
- Can add Spring Cloud Gateway or similar later
- Not critical for development and testing

**Consequences**:
- No centralized routing, rate limiting, or authentication
- Each service handles security independently
- Future gateway introduction may require API changes

---

## ADR-016: Service Discovery Strategy

**Date**: 2025-12-16  
**Status**: Deferred  
**Context**: Services need to locate each other.

**Decision**:
- Use direct URL configuration (environment variables)
- No service discovery (Eureka, Consul) for MVP
- Can evolve to dynamic discovery if needed

**Rationale**:
- Direct URLs are simpler for development environment
- Avoids infrastructure dependency
- Sufficient for small number of services
- Can add discovery layer without major refactoring

**Consequences**:
- Manual configuration management required
- No automatic failover or load balancing
- Configuration changes require service restart
- Simpler deployment and debugging

---

## ADR-017: Audit Events Strategy

**Date**: 2025-12-16  
**Status**: Deferred  
**Context**: Need audit trail of domain events.

**Decision**:
- Defer audit event emission to later phases
- Focus on core functionality first
- Design services with event emission in mind

**Rationale**:
- Event infrastructure adds complexity
- Not critical for MVP functionality
- Services can evolve to emit events without major refactoring

**Consequences**:
- No audit trail or event replay capability initially
- May need to refactor for event emission later
- Simpler initial implementation

---

## Summary

These decisions establish the foundation for the Digital Marketplace platform:
- **Modern stack**: Java 21, Spring Boot 3.x, PostgreSQL
- **Microservices**: Independent services with dedicated databases
- **API-first**: OpenAPI contracts, REST APIs
- **Security**: JWT with RS256, BCrypt passwords
- **Observability**: Structured logging, correlation IDs
- **Simplicity**: Direct URLs, manual processes for MVP
- **Evolution**: Deferred gateway, discovery, and events to later phases

These decisions prioritize rapid development, clear patterns, and the ability to evolve as needs emerge.
