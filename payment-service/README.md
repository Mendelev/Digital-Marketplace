# Payment Service

The Payment Service provides secure payment processing functionality for the Digital Marketplace platform. It simulates a payment gateway with support for authorization, capture, refund, and void operations, following industry-standard payment workflows.

## Features

- **Payment lifecycle management**: Complete payment workflow from authorization to capture/refund
- **Partial operations**: Support for partial captures and partial refunds
- **Idempotency**: Prevents duplicate transactions using idempotency keys
- **Payment simulation**: Configurable success/failure rates for testing different scenarios
- **Transaction history**: Complete audit trail of all payment operations
- **State machine validation**: Enforces proper payment state transitions
- **JWT authentication**: Secure user authentication for payment queries
- **Service-to-service auth**: Shared secret authentication for internal API calls
- **Distributed tracing**: Correlation ID propagation via X-Correlation-ID header
- **Event placeholders**: Ready for future event-driven architecture integration

## Technology Stack

- **Spring Boot 3.2.1** with Java 21
- **PostgreSQL** for payment data storage
- **Flyway** for database migrations
- **Spring Data JPA** for repository layer
- **JWT (JJWT 0.12.3)** for token validation
- **Resilience4j** for circuit breakers
- **Logstash Logback Encoder** for structured JSON logging
- **Springdoc OpenAPI** for API documentation

## Architecture

### Payment State Machine

The Payment Service implements a strict state machine to ensure payment integrity:

```
INITIATED → AUTHORIZED → CAPTURED → REFUNDED
         ↓             ↓
      FAILED        VOIDED
```

**State Transitions:**
- `INITIATED`: Payment created, awaiting authorization
- `AUTHORIZED`: Funds authorized, can be captured or voided
- `CAPTURED`: Funds captured, can be refunded
- `FAILED`: Terminal state (authorization or capture failed)
- `VOIDED`: Terminal state (authorization cancelled before capture)
- `REFUNDED`: Terminal state (funds returned to customer)

### Partial Operations Support

**Partial Captures:**
- Can capture in multiple transactions up to the authorized amount
- Tracks `capturedAmount` separately from total `amount`
- Example: Authorize $100, capture $50, capture $30, void remaining $20

**Partial Refunds:**
- Can refund in multiple transactions up to the captured amount
- Tracks `refundedAmount` separately from captured amount
- Example: Capture $100, refund $30, refund $20, $50 remains

### Database Schema

```sql
-- Payments table
CREATE TABLE payments (
    payment_id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    provider VARCHAR(20) DEFAULT 'MOCK',
    captured_amount DECIMAL(19,4) DEFAULT 0,
    refunded_amount DECIMAL(19,4) DEFAULT 0,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Payment transactions table
CREATE TABLE payment_transactions (
    transaction_id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    provider_reference VARCHAR(100),
    error_message VARCHAR(500),
    idempotency_key VARCHAR(100) UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
```

## Quick Start

Get the Payment Service up and running in minutes:

```bash
# 1. Start PostgreSQL
docker run -d \
  --name payment-db \
  -p 5433:5432 \
  -e POSTGRES_DB=payment_db \
  -e POSTGRES_USER=payment_user \
  -e POSTGRES_PASSWORD=payment_pass \
  postgres:16-alpine

# 2. Verify PostgreSQL is running
psql -h localhost -p 5433 -U payment_user -d payment_db

# 3. Navigate to payment-service directory
cd payment-service

# 4. Build the service
mvn clean install

# 5. Run the service
mvn spring-boot:run

# 6. Access Swagger UI
open http://localhost:8083/swagger-ui.html
# Or visit: http://localhost:8083/swagger-ui.html

# 7. Test the health endpoint
curl http://localhost:8083/actuator/health
```

The service will automatically:
- Create database tables using Flyway migrations
- Start listening for payment requests on port 8083
- Enable JWT validation for user authentication
- Configure payment simulation with default success rates

## Prerequisites

1. **Java 21** or higher
2. **Maven 3.6+**
3. **PostgreSQL 16** running on `localhost:5433`
4. **Auth Service** running on `localhost:8080` (for JWT public key)
5. **Order Service** running on `localhost:8084` (optional, for order validation)

## Setup Instructions

### 1. Start PostgreSQL

```bash
# Using Docker
docker run -d \
  --name payment-db \
  -p 5433:5432 \
  -e POSTGRES_DB=payment_db \
  -e POSTGRES_USER=payment_user \
  -e POSTGRES_PASSWORD=payment_pass \
  postgres:16-alpine

# Verify PostgreSQL is running
docker ps | grep payment-db
```

### 2. Verify Shared DTOs

The Payment Service depends on shared DTOs. Build them first:

```bash
cd common/shared-dtos
mvn clean install
```

### 3. Build the Service

```bash
cd payment-service
mvn clean install
```

### 4. Run the Service

```bash
mvn spring-boot:run
```

The service will start on **port 8083**.

### 5. Verify Startup

Check the logs for successful migration:

```
INFO - Flyway migration completed successfully
INFO - Started PaymentServiceApplication
```

Verify database tables were created:

```bash
psql -h localhost -p 5433 -U payment_user -d payment_db -c "\dt"
```

You should see:
- `payments`
- `payment_transactions`
- `flyway_schema_history`

## Docker

### Start PostgreSQL (Local)

```bash
cd /Users/yuri.camargo/DevPro/Practice_projects/Digital-Marketplace/payment-service
docker-compose up -d
```

### Build and Run Payment Service

```bash
cd /Users/yuri.camargo/DevPro/Practice_projects/Digital-Marketplace/payment-service
docker-compose -f docker-compose-full.yml up -d --build
```

The container maps port 8083 inside the container to port 8087 on the host to avoid conflicts with cart-service.

## Testing

Manual test steps live in `payment-service/TESTING.md`.

## API Endpoints

### Base URL
```
http://localhost:8083/api/v1/payments
```

### Swagger UI
```
http://localhost:8083/swagger-ui.html
```

---

### 1. Create Payment

**Endpoint:** `POST /api/v1/payments`

**Description:** Create a new payment intent (service-to-service only).

**Authentication:** Requires `X-Service-Secret` header

**Request Body:**
```json
{
  "orderId": "123e4567-e89b-12d3-a456-426614174000",
  "userId": "123e4567-e89b-12d3-a456-426614174001",
  "amount": 99.99,
  "currency": "USD"
}
```

**Response:**
```json
{
  "paymentId": "123e4567-e89b-12d3-a456-426614174002",
  "orderId": "123e4567-e89b-12d3-a456-426614174000",
  "userId": "123e4567-e89b-12d3-a456-426614174001",
  "status": "INITIATED",
  "amount": 99.99,
  "capturedAmount": 0.00,
  "refundedAmount": 0.00,
  "currency": "USD",
  "provider": "MOCK",
  "createdAt": "2025-12-19T10:30:00.000+00:00",
  "updatedAt": "2025-12-19T10:30:00.000+00:00"
}
```

---

### 2. Authorize Payment

**Endpoint:** `POST /api/v1/payments/{paymentId}/authorize`

**Description:** Authorize payment (hold funds).

**Authentication:** Requires `X-Service-Secret` header

**Request Body (optional):**
```json
{
  "idempotencyKey": "unique-key-123"
}
```

**Response:**
```json
{
  "paymentId": "123e4567-e89b-12d3-a456-426614174002",
  "orderId": "123e4567-e89b-12d3-a456-426614174000",
  "userId": "123e4567-e89b-12d3-a456-426614174001",
  "status": "AUTHORIZED",
  "amount": 99.99,
  "capturedAmount": 0.00,
  "refundedAmount": 0.00,
  "currency": "USD",
  "provider": "MOCK",
  "createdAt": "2025-12-19T10:30:00.000+00:00",
  "updatedAt": "2025-12-19T10:31:00.000+00:00"
}
```

**Simulation:**
- 95% success rate by default (configurable)
- Randomly simulates failures: "Insufficient funds", "Card declined", etc.

---

### 3. Capture Payment

**Endpoint:** `POST /api/v1/payments/{paymentId}/capture`

**Description:** Capture authorized payment (transfer funds). Supports partial capture.

**Authentication:** Requires `X-Service-Secret` header

**Request Body:**
```json
{
  "amount": 99.99,
  "idempotencyKey": "unique-key-124"
}
```

**Response:**
```json
{
  "paymentId": "123e4567-e89b-12d3-a456-426614174002",
  "status": "CAPTURED",
  "amount": 99.99,
  "capturedAmount": 99.99,
  "refundedAmount": 0.00,
  "currency": "USD"
}
```

**Partial Capture Example:**
```json
// First capture: $50 of $100 authorized
{
  "amount": 50.00,
  "idempotencyKey": "capture-1"
}

// Second capture: $30 more
{
  "amount": 30.00,
  "idempotencyKey": "capture-2"
}

// Payment status: CAPTURED with capturedAmount = 80.00
// Remaining $20 can still be voided
```

---

### 4. Refund Payment

**Endpoint:** `POST /api/v1/payments/{paymentId}/refund`

**Description:** Refund captured payment. Supports partial refunds.

**Authentication:** Requires `X-Service-Secret` header OR admin JWT

**Request Body:**
```json
{
  "amount": 50.00,
  "reason": "Customer requested refund",
  "idempotencyKey": "unique-key-125"
}
```

**Response:**
```json
{
  "paymentId": "123e4567-e89b-12d3-a456-426614174002",
  "status": "REFUNDED",
  "amount": 99.99,
  "capturedAmount": 99.99,
  "refundedAmount": 50.00,
  "currency": "USD"
}
```

---

### 5. Void Payment

**Endpoint:** `POST /api/v1/payments/{paymentId}/void`

**Description:** Void payment authorization (cancel hold before capture).

**Authentication:** Requires `X-Service-Secret` header

**Request Body (optional):**
```json
{
  "idempotencyKey": "unique-key-126"
}
```

**Response:**
```json
{
  "paymentId": "123e4567-e89b-12d3-a456-426614174002",
  "status": "VOIDED",
  "amount": 99.99,
  "capturedAmount": 0.00,
  "refundedAmount": 0.00,
  "currency": "USD"
}
```

---

### 6. Get Payment

**Endpoint:** `GET /api/v1/payments/{paymentId}`

**Description:** Get payment details by ID.

**Authentication:** Requires JWT token (user must own the payment or be admin)

**Response:**
```json
{
  "paymentId": "123e4567-e89b-12d3-a456-426614174002",
  "orderId": "123e4567-e89b-12d3-a456-426614174000",
  "userId": "123e4567-e89b-12d3-a456-426614174001",
  "status": "CAPTURED",
  "amount": 99.99,
  "capturedAmount": 99.99,
  "refundedAmount": 0.00,
  "currency": "USD",
  "provider": "MOCK",
  "createdAt": "2025-12-19T10:30:00.000+00:00",
  "updatedAt": "2025-12-19T10:32:00.000+00:00"
}
```

---

### 7. Get Payment by Order ID

**Endpoint:** `GET /api/v1/payments/order/{orderId}`

**Description:** Get payment for a specific order.

**Authentication:** Requires JWT token

---

### 8. Get User Payments

**Endpoint:** `GET /api/v1/payments/user/{userId}`

**Description:** Get all payments for a user.

**Authentication:** Requires JWT token (user must match or be admin)

**Response:**
```json
[
  {
    "paymentId": "...",
    "orderId": "...",
    "status": "CAPTURED",
    "amount": 99.99,
    ...
  },
  {
    "paymentId": "...",
    "orderId": "...",
    "status": "REFUNDED",
    "amount": 49.99,
    ...
  }
]
```

---

### 9. Get Payment Transactions

**Endpoint:** `GET /api/v1/payments/{paymentId}/transactions`

**Description:** Get transaction history for a payment.

**Authentication:** Requires JWT token

**Response:**
```json
[
  {
    "transactionId": "...",
    "paymentId": "...",
    "type": "AUTHORIZE",
    "status": "SUCCESS",
    "amount": 99.99,
    "currency": "USD",
    "providerReference": "mock-txn-a1b2c3d4",
    "errorMessage": null,
    "createdAt": "2025-12-19T10:31:00.000+00:00"
  },
  {
    "transactionId": "...",
    "paymentId": "...",
    "type": "CAPTURE",
    "status": "SUCCESS",
    "amount": 99.99,
    "currency": "USD",
    "providerReference": "mock-txn-e5f6g7h8",
    "errorMessage": null,
    "createdAt": "2025-12-19T10:32:00.000+00:00"
  }
]
```

---

## Testing the Service

### Step 1: Start Required Services

Make sure the following services are running:

1. **PostgreSQL** on `localhost:5433`
2. **Auth Service** on `localhost:8080`
3. **Payment Service** on `localhost:8083`

### Step 2: Get Authentication Credentials

For service-to-service calls, use the shared secret (configured in `application.yml`):
```
X-Service-Secret: dev-secret-change-in-production
```

For user calls, obtain a JWT token from the Auth Service:
```bash
# Login to get JWT token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'

# Extract accessToken from response
```

### Step 3: Test Complete Payment Flow

#### Create Payment

```bash
curl -X POST http://localhost:8083/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: dev-secret-change-in-production" \
  -d '{
    "orderId": "123e4567-e89b-12d3-a456-426614174000",
    "userId": "123e4567-e89b-12d3-a456-426614174001",
    "amount": 99.99,
    "currency": "USD"
  }'
```

Save the `paymentId` from the response.

#### Authorize Payment

```bash
curl -X POST http://localhost:8083/api/v1/payments/<payment-id>/authorize \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: dev-secret-change-in-production" \
  -d '{
    "idempotencyKey": "auth-key-001"
  }'
```

**Note:** May fail randomly (5% failure rate). Retry if it fails to test the simulation.

#### Capture Payment

```bash
curl -X POST http://localhost:8083/api/v1/payments/<payment-id>/capture \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: dev-secret-change-in-production" \
  -d '{
    "amount": 99.99,
    "idempotencyKey": "capture-key-001"
  }'
```

#### Get Payment Status

```bash
curl http://localhost:8083/api/v1/payments/<payment-id> \
  -H "Authorization: Bearer <jwt-token>"
```

#### Get Transaction History

```bash
curl http://localhost:8083/api/v1/payments/<payment-id>/transactions \
  -H "Authorization: Bearer <jwt-token>"
```

### Step 4: Test Partial Operations

#### Partial Capture Flow

```bash
# 1. Create and authorize $100 payment
curl -X POST http://localhost:8083/api/v1/payments/<payment-id>/authorize \
  -H "X-Service-Secret: dev-secret-change-in-production"

# 2. Capture $50
curl -X POST http://localhost:8083/api/v1/payments/<payment-id>/capture \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: dev-secret-change-in-production" \
  -d '{
    "amount": 50.00,
    "idempotencyKey": "capture-1"
  }'

# 3. Capture another $30
curl -X POST http://localhost:8083/api/v1/payments/<payment-id>/capture \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: dev-secret-change-in-production" \
  -d '{
    "amount": 30.00,
    "idempotencyKey": "capture-2"
  }'

# Payment now has: amount=100, capturedAmount=80, status=CAPTURED
# Remaining $20 can be voided
```

#### Partial Refund Flow

```bash
# 1. After full capture of $100

# 2. Refund $30
curl -X POST http://localhost:8083/api/v1/payments/<payment-id>/refund \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: dev-secret-change-in-production" \
  -d '{
    "amount": 30.00,
    "reason": "Partial refund - one item returned",
    "idempotencyKey": "refund-1"
  }'

# 3. Refund another $20
curl -X POST http://localhost:8083/api/v1/payments/<payment-id>/refund \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: dev-secret-change-in-production" \
  -d '{
    "amount": 20.00,
    "reason": "Additional refund",
    "idempotencyKey": "refund-2"
  }'

# Payment now has: capturedAmount=100, refundedAmount=50
# Can still refund up to $50 more
```

### Step 5: Test Idempotency

```bash
# Send same request twice with same idempotency key
curl -X POST http://localhost:8083/api/v1/payments/<payment-id>/authorize \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: dev-secret-change-in-production" \
  -d '{
    "idempotencyKey": "duplicate-test-001"
  }'

# Second request with same key - should return same result without double processing
curl -X POST http://localhost:8083/api/v1/payments/<payment-id>/authorize \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: dev-secret-change-in-production" \
  -d '{
    "idempotencyKey": "duplicate-test-001"
  }'
```

### Step 6: Test Error Scenarios

#### Invalid State Transition

```bash
# Try to capture without authorization
curl -X POST http://localhost:8083/api/v1/payments/<payment-id>/capture \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: dev-secret-change-in-production" \
  -d '{
    "amount": 99.99
  }'

# Should return 400 Bad Request
```

#### Amount Validation

```bash
# Try to capture more than authorized
curl -X POST http://localhost:8083/api/v1/payments/<payment-id>/capture \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: dev-secret-change-in-production" \
  -d '{
    "amount": 150.00
  }'

# Should return 400 Bad Request
```

### Step 7: Verify Correlation ID Tracing

```bash
curl -X POST http://localhost:8083/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: dev-secret-change-in-production" \
  -H "X-Correlation-ID: test-correlation-123" \
  -d '{...}' \
  -v
```

Check the response headers for the `X-Correlation-ID` header, and verify it appears in the logs.

---

## Configuration

Key configuration properties in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/payment_db
    username: payment_user
    password: payment_pass

  jpa:
    hibernate:
      ddl-auto: validate  # Use Flyway for migrations

  flyway:
    enabled: true
    baseline-on-migrate: true

server:
  port: 8083

# JWT Configuration
jwt:
  public-key-endpoint: http://localhost:8080/api/v1/auth/public-key
  public-key-cache-ttl-minutes: 60

# Auth Service
auth-service:
  base-url: http://localhost:8080
  shared-secret: dev-secret-change-in-production

# Payment Simulation
payment-simulation:
  authorization-success-rate: 0.95  # 95% success
  capture-success-rate: 0.98        # 98% success
  refund-success-rate: 0.99         # 99% success
  void-success-rate: 0.99           # 99% success
  simulation-delay-ms: 100          # 100ms delay

# Service-to-service authentication
service:
  shared-secret: dev-secret-change-in-production

# Circuit Breaker
resilience4j:
  circuitbreaker:
    instances:
      authService:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
```

---

## Monitoring and Logging

### Logs Location

Logs are written to:
- **Console**: Structured format with correlation IDs
- **Application logs**: All payment operations logged

### Log Format

```json
{
  "timestamp": "2025-12-19T10:30:45.123Z",
  "level": "INFO",
  "service": "payment-service",
  "correlationId": "abc-123-def",
  "operation": "authorizePayment",
  "paymentId": "123e4567-...",
  "logger": "com.marketplace.payment.service.PaymentService",
  "message": "Payment authorized successfully: paymentId=..."
}
```

### Database Monitoring

```bash
# Check payment statistics
psql -h localhost -p 5433 -U payment_user -d payment_db -c "
  SELECT status, COUNT(*), SUM(amount)
  FROM payments
  GROUP BY status;
"

# Check recent transactions
psql -h localhost -p 5433 -U payment_user -d payment_db -c "
  SELECT type, status, COUNT(*)
  FROM payment_transactions
  WHERE created_at > NOW() - INTERVAL '1 hour'
  GROUP BY type, status;
"

# Find payments with partial captures
psql -h localhost -p 5433 -U payment_user -d payment_db -c "
  SELECT payment_id, amount, captured_amount, refunded_amount, status
  FROM payments
  WHERE captured_amount < amount AND captured_amount > 0;
"
```

---

## Troubleshooting

### Issue: Database connection failed

**Solution:** Check PostgreSQL connection:
```bash
psql -h localhost -p 5433 -U payment_user -d payment_db
```

Verify credentials in `application.yml` match database configuration.

### Issue: Flyway migration failed

**Solution:**
1. Check migration scripts in `src/main/resources/db/migration/`
2. Verify no manual schema changes were made
3. Check Flyway history:
   ```bash
   psql -h localhost -p 5433 -U payment_user -d payment_db -c "SELECT * FROM flyway_schema_history;"
   ```
4. If needed, repair Flyway:
   ```bash
   mvn flyway:repair
   ```

### Issue: JWT validation failing

**Solution:**
1. Verify Auth Service is running: `curl http://localhost:8080/actuator/health`
2. Check public key endpoint: `curl http://localhost:8080/api/v1/auth/public-key`
3. Review circuit breaker status in logs
4. Clear public key cache and retry

### Issue: All payments failing (simulation)

**Solution:** Adjust success rates in `application.yml`:
```yaml
payment-simulation:
  authorization-success-rate: 1.0  # 100% success for testing
  capture-success-rate: 1.0
  refund-success-rate: 1.0
  void-success-rate: 1.0
```

### Issue: Duplicate payment creation

**Solution:** This is expected behavior - one payment per order (enforced by unique constraint on `order_id`). The application will return 409 Conflict if a payment already exists for an order.

---

## Architecture Patterns

### State Machine Pattern
- Strict validation of state transitions
- Business methods on domain entities
- IllegalStateException for invalid transitions

### Idempotency Pattern
- Unique idempotency keys stored with transactions
- Database-level uniqueness constraint
- Return existing result for duplicate requests

### Optimistic Locking
- Version column on Payment entity
- Prevents lost updates in concurrent scenarios
- JPA automatic version increment

### Partial Operations
- Separate tracking of captured/refunded amounts
- Validation against authorized/captured amounts
- Multiple transactions per payment

### Event Sourcing (Prepared)
- TODO comments for event emission points
- Ready for Kafka/messaging integration
- Complete transaction history preserved

### Circuit Breaker
- Resilience4j for Auth Service calls
- Fallback to cached public key
- Prevents cascading failures

### Correlation ID Tracing
- Propagated via X-Correlation-ID header
- Stored in MDC for all log entries
- Returned in response headers
- Enables distributed tracing

---

## Integration with Order Service

The Payment Service is designed to be called by the Order Service during checkout:

**Typical Order Flow:**
1. **Order Service**: Create order → `POST /api/v1/payments` (create payment)
2. **Order Service**: Start checkout → `POST /api/v1/payments/{id}/authorize`
3. **Order Service**: Inventory confirmed → `POST /api/v1/payments/{id}/capture`
4. **Order Service**: Order cancelled → `POST /api/v1/payments/{id}/void` or `/refund`

**Required Configuration in Order Service:**
```yaml
payment-service:
  base-url: http://localhost:8083
  shared-secret: dev-secret-change-in-production  # Must match Payment Service
```

---

## Future Enhancements

- Real payment gateway integration (Stripe, PayPal, Square)
- Webhook notifications for payment status changes
- Scheduled jobs for expired authorization cleanup
- Payment method tokenization (credit cards, ACH)
- 3D Secure / PCI DSS compliance
- Multi-currency support with conversion rates
- Recurring payments / subscriptions
- Payment analytics dashboard
- Fraud detection integration
- Event emission to Kafka for audit/analytics
- Performance optimizations (caching, connection pooling)
- Advanced security (encryption at rest, PCI compliance)

---

## API Documentation

Full API documentation is available via Swagger UI:

**URL:** http://localhost:8083/swagger-ui.html

---

## License

Copyright © 2025 Digital Marketplace. All rights reserved.
