# Auth Service - Digital Marketplace

Authentication and Authorization microservice for the Digital Marketplace platform.

## Overview

The Auth Service provides secure user authentication, JWT token management, and password recovery functionality. Built with Java 21, Spring Boot 3.x, and PostgreSQL, it follows microservices best practices with independent deployment, dedicated database, and comprehensive observability.

## Features

- ✅ User registration with automatic User Service integration
- ✅ Email/password authentication with BCrypt hashing
- ✅ JWT token issuance (RS256 algorithm)
  - Short-lived access tokens (15 minutes)
  - Long-lived refresh tokens (7 days)
- ✅ Token refresh and rotation
- ✅ Password reset flow with time-limited tokens
- ✅ Account lockout after failed login attempts
- ✅ Token validation endpoint for other services
- ✅ Public key distribution for JWT verification
- ✅ Manual token cleanup endpoint
- ✅ Correlation ID propagation for distributed tracing
- ✅ Structured JSON logging
- ✅ Circuit breaker for User Service calls
- ✅ Comprehensive error handling with standard format

## Technology Stack

- **Java**: 21
- **Framework**: Spring Boot 3.2.1
- **Build Tool**: Maven
- **Database**: PostgreSQL 16
- **Migration**: Flyway
- **JWT Library**: jjwt 0.12.3 (RS256)
- **Password Hashing**: BCrypt (strength 12)
- **API Documentation**: Springdoc OpenAPI / Swagger
- **Resilience**: Resilience4j Circuit Breaker
- **Logging**: Logstash Logback Encoder (JSON format)
- **Testing**: JUnit 5, Mockito

## Project Structure

```
auth-service/
├── src/
│   ├── main/
│   │   ├── java/com/marketplace/auth/
│   │   │   ├── config/              # Configuration classes
│   │   │   ├── domain/
│   │   │   │   ├── model/           # JPA entities
│   │   │   │   └── repository/      # Spring Data repositories
│   │   │   ├── service/             # Business logic
│   │   │   ├── client/              # External service clients
│   │   │   ├── controller/          # REST controllers
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── exception/           # Custom exceptions
│   │   │   ├── filter/              # Servlet filters
│   │   │   ├── aspect/              # AOP aspects for logging
│   │   │   └── util/                # Utility classes
│   │   └── resources/
│   │       ├── db/migration/        # Flyway SQL scripts
│   │       ├── keys/                # RSA key pair (private_key.pem, public_key.pem)
│   │       ├── application.yml      # Application configuration
│   │       └── logback-spring.xml   # Logging configuration
│   └── test/
│       └── java/com/marketplace/auth/   # Unit tests (to be implemented)
├── mockoon/
│   └── user-service-mock.json       # User Service mock configuration
├── docker-compose.yml               # PostgreSQL + Mockoon containers
└── pom.xml                          # Maven dependencies
```

## Prerequisites

- **Java Development Kit (JDK)** 21 or higher
- **Maven** 3.8+
- **Docker** and **Docker Compose**
- **Git**

## Implementation Status

### ✅ Completed

1. **Project Infrastructure**
   - Maven POM with all required dependencies (including Spring Security Crypto)
   - Spring Boot application class with JPA auditing
   - Docker Compose with PostgreSQL and Mockoon
   - Application configuration (application.yml)
   - Structured JSON logging configuration (logback-spring.xml)

2. **Database Layer**
   - Flyway migrations for:
     - `credentials` table (authentication data)
     - `refresh_tokens` table (token management)
     - `password_reset_tokens` table (password recovery)
   - Proper indexes for query optimization
   - UUID primary keys with automatic generation

3. **Domain Layer**
   - `Credential` entity with status management and business methods
   - `RefreshToken` entity with expiration/revocation logic
   - `PasswordResetToken` entity with usage tracking
   - Spring Data JPA repositories with custom queries
   - `CredentialStatus` enum

4. **Configuration**
   - `JwtProperties` - JWT configuration binding
   - `UserServiceProperties` - User Service client configuration
   - `SecurityConfig` - BCrypt password encoder bean
   - `RestTemplateConfig` - RestTemplate with correlation ID interceptor
   - Resilience4j circuit breaker configuration

5. **Security & JWT**
   - `KeyPairGeneratorUtil` - RSA 2048-bit key pair generator (executed)
   - `JwtService` - Complete JWT token management with RS256
   - RSA key pair generated in `src/main/resources/keys/`

6. **Service Layer**
   - `AuthService` - Complete authentication business logic:
     - User registration with User Service integration
     - Login with failed attempt tracking and account lockout
     - Token refresh with rotation
     - Password reset flow (request and confirmation)
     - Token validation
     - Token cleanup
   - `JwtService` - Token generation, validation, and public key distribution

7. **Client Layer**
   - `UserServiceClient` - HTTP client for User Service
   - Circuit breaker integration with Resilience4j
   - Error handling and fallback methods

8. **Filter & Aspect**
   - `CorrelationIdFilter` - X-Correlation-ID extraction/generation
   - `LoggingAspect` - Service method logging with execution time and sanitized arguments

9. **DTOs** (Data Transfer Objects)
   - Request DTOs: `RegisterRequest`, `LoginRequest`, `RefreshTokenRequest`, `ForgotPasswordRequest`, `ResetPasswordRequest`, `ValidateTokenRequest`, `CleanupTokensRequest`, `CreateUserRequest`
   - Response DTOs: `AuthResponse`, `TokenValidationResponse`, `MessageResponse`, `CleanupTokensResponse`, `UserResponse`, `ErrorResponse`
   - All DTOs use Java records with Jakarta validation

10. **Exception Handling**
    - Custom exception classes: `DuplicateEmailException`, `InvalidCredentialsException`, `AccountLockedException`, `TokenExpiredException`, `InvalidTokenException`, `ResourceNotFoundException`, `UserServiceException`
    - `GlobalExceptionHandler` - @RestControllerAdvice with proper HTTP status codes
    - Standardized error response format with correlation IDs

11. **REST Controllers**
    - `AuthController` - All authentication endpoints:
      - POST /api/v1/auth/register (201 Created)
      - POST /api/v1/auth/login (200 OK)
      - POST /api/v1/auth/refresh (200 OK)
      - POST /api/v1/auth/forgot-password (200 OK)
      - POST /api/v1/auth/reset-password (200 OK)
      - POST /api/v1/auth/validate (200 OK)
      - GET /api/v1/auth/public-key (200 OK, text/plain)
      - DELETE /api/v1/auth/tokens/cleanup (200 OK)
    - OpenAPI/Swagger annotations for documentation

12. **Documentation**
    - Architecture Decision Records (ADR) in root
    - Mockoon configuration for User Service stub
    - Common error response schema
    - Comprehensive README

### 🚧 To Be Implemented

The following components remain to complete the Auth Service:

1. **Unit Tests**
   - Service layer tests with Mockito
   - Repository tests with @DataJpaTest
   - Controller tests with MockMvc
   - JWT service tests
   - Error handling tests
   - Circuit breaker tests

2. **Integration Tests** (Optional Enhancement)
   - Testcontainers for PostgreSQL
   - End-to-end API tests
   - User Service mock integration tests

3. **OpenAPI Specification YAML** (Optional Enhancement)
   - Standalone `auth-api.yaml` file
   - Complete API contract definition
   - Request/response example payloads

## Quick Start

### 1. Start Infrastructure

```bash
docker-compose up -d
```

This starts:
- PostgreSQL on port 5432
- Mockoon (User Service mock) on port 3001

### 2. Run Flyway Migrations

```bash
mvn flyway:migrate
```

### 3. Start the Application

```bash
mvn spring-boot:run
```

The service will be available at: http://localhost:8080

### 4. Access Swagger UI

Open http://localhost:8080/swagger-ui.html to explore the API documentation.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Authenticate user |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/forgot-password` | Request password reset |
| POST | `/api/v1/auth/reset-password` | Reset password with token |
| POST | `/api/v1/auth/validate` | Validate JWT token |
| GET | `/api/v1/auth/public-key` | Get RSA public key |
| DELETE | `/api/v1/auth/tokens/cleanup` | Cleanup old tokens |

## Configuration

### Database

Edit `application.yml` to configure PostgreSQL connection:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_db
    username: auth_user
    password: auth_pass
```

### JWT Settings

```yaml
jwt:
  private-key-path: classpath:keys/private_key.pem
  public-key-path: classpath:keys/public_key.pem
  access-token-expiration-minutes: 15
  refresh-token-expiration-days: 7
```

### User Service

```yaml
user-service:
  base-url: http://localhost:3001
  connect-timeout-ms: 5000
  read-timeout-ms: 10000
```

## Testing

Unit tests are not yet implemented. To run tests once they are added:

```bash
mvn test
```

Run with coverage:

```bash
mvn clean test jacoco:report
```

## Development Workflow

1. Ensure Docker containers are running
2. Make code changes
3. Run tests: `mvn test`
4. Start application: `mvn spring-boot:run`
5. Test endpoints via Swagger UI or curl

## Logging

Logs are written in structured JSON format with correlation IDs:

```json
{
  "timestamp": "2025-12-16T10:30:45.123Z",
  "level": "INFO",
  "logger": "com.marketplace.auth.service.AuthService",
  "message": "User registered successfully",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "operation": "register",
  "service": "auth-service"
}
```

## Error Handling

All errors return a standard format:

```json
{
  "timestamp": "2025-12-16T10:30:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Email address is already registered",
  "path": "/api/v1/auth/register",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

## Security Considerations

- Passwords are hashed with BCrypt (strength 12)
- JWT tokens signed with RS256 (2048-bit keys)
- Refresh tokens are hashed before storage
- Account lockout after 5 failed login attempts
- Password reset tokens expire after 1 hour
- Tokens are validated for signature and expiration
- No sensitive data in logs (passwords sanitized)

## Circuit Breaker

The User Service client uses Resilience4j circuit breaker:
- Sliding window: 10 calls
- Failure threshold: 50%
- Wait in open state: 10 seconds
- Half-open test calls: 3

## Troubleshooting

### Database Connection Error

Ensure PostgreSQL container is running:
```bash
docker-compose ps
docker-compose logs postgres
```

### Port Already in Use

Change ports in `docker-compose.yml` or `application.yml` if 5432 or 8080 are occupied.

### Key Files Not Found

Generate RSA keys using the KeyPairGeneratorUtil (see Quick Start).

### User Service Connection Error

Check Mockoon container:
```bash
docker-compose logs mockoon
curl http://localhost:3001/health
```

## Next Steps

1. **Complete Implementation**: Implement the remaining components listed in "To Be Implemented"
2. **Testing**: Write comprehensive unit tests for all components
3. **Integration Testing**: Add Testcontainers for integration tests
4. **API Documentation**: Complete OpenAPI specification
5. **Performance Testing**: Load test authentication endpoints
6. **Security Audit**: Review security implementation
7. **Monitoring**: Add Prometheus metrics and health checks

## Related Services

- **User Service**: Manages user profiles and addresses
- **API Gateway**: (Future) Central entry point for all services

## References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [jjwt Library](https://github.com/jwtk/jjwt)
- [Flyway Migrations](https://flywaydb.org/)
- [Resilience4j](https://resilience4j.readme.io/)
- [Architecture Decision Records](../ARCHITECTURE_DECISIONS.md)

## License

Part of the Digital Marketplace platform (sample project).
