# Order Service

Order management and orchestration service for Digital Marketplace. Handles the complete order lifecycle from creation to delivery, coordinating with payment and inventory systems.

## Overview

The Order Service is a critical component of the Digital Marketplace platform that:

- Creates orders from shopping carts with frozen product/address snapshots
- Orchestrates payment authorization and capture
- Manages inventory reservations with time-to-live (TTL)
- Implements a state machine for order status transitions
- Publishes order events to Kafka for event-driven architecture
- Provides REST APIs for order management

## Features

### Order Management
- Create orders from cart with product and address snapshots
- Retrieve order details and history
- Cancel orders (before shipping)
- Track order status through lifecycle

### Payment Integration
- Mock payment service with configurable success rate (90% default)
- Payment authorization, capture, refund, and void operations
- Payment transaction audit trail

### Inventory Management
- Mock inventory service with automatic stock creation
- Inventory reservations with 15-minute TTL
- Stock reservation, confirmation, and release

### State Machine
- Enforces valid order status transitions
- Prevents invalid state changes
- 9 order statuses with defined transition paths

### Event Sourcing
- Publishes events to Kafka for all order operations
- Event audit trail in database
- Sequence numbering for event ordering
- Events: OrderCreated, OrderConfirmed, OrderCancelled, OrderStatusChanged, OrderPaymentFailed

## Technology Stack

- **Java 21**
- **Spring Boot 3.2.1**
- **PostgreSQL** - Order data storage
- **Flyway** - Database migrations
- **Apache Kafka** - Event streaming
- **Spring Cloud Stream** - Kafka integration
- **Resilience4j** - Circuit breakers for service communication
- **OpenAPI/Swagger** - API documentation

## Prerequisites

- Java 21 or higher
- Maven 3.8+
- PostgreSQL 14+
- Kafka 3.x
- Running instances of:
  - Cart Service (port 8083)
  - User Service (port 8081)

## Configuration

### Database Setup

Create PostgreSQL database:

```sql
CREATE DATABASE order_db;
CREATE USER order_user WITH PASSWORD 'order_pass';
GRANT ALL PRIVILEGES ON DATABASE order_db TO order_user;
```

### Environment Variables

```bash
# Database
DB_USERNAME=order_user
DB_PASSWORD=order_pass

# Kafka
KAFKA_BROKERS=localhost:9092

# Service URLs
CART_SERVICE_URL=http://localhost:8083
USER_SERVICE_URL=http://localhost:8081

# Application Port
PORT=8086
```

### Application Properties

Key configuration in `application.yml`:

```yaml
server:
  port: 8086

spring:
  datasource:
    url: jdbc:postgresql://localhost:5466/order_db

order-service:
  flat-shipping-rate: 9.99
  payment-success-rate: 90
  reservation-ttl-minutes: 15
```

## Database Schema

### Core Tables

1. **orders** - Order header with amounts and address snapshots (JSONB)
2. **order_items** - Order line items with product snapshots
3. **order_events** - Event sourcing table for audit trail

### Payment Tables

4. **payments** - Payment records
5. **payment_transactions** - Payment transaction audit trail

### Inventory Tables

6. **stock_items** - Mock inventory stock levels
7. **reservations** - Inventory reservations with expiry
8. **reservation_lines** - Reservation line items per SKU

## API Endpoints

Base URL: `http://localhost:8086/api/v1/orders`

### Create Order
```http
POST /api/v1/orders
Content-Type: application/json

{
  "userId": "uuid",
  "cartId": "uuid",
  "shippingAddressId": "uuid",
  "billingAddressId": "uuid"
}
```

### Get Order
```http
GET /api/v1/orders/{orderId}
```

### Get User Orders
```http
GET /api/v1/orders/user/{userId}?page=0&size=20
```

### Cancel Order
```http
POST /api/v1/orders/{orderId}/cancel?reason=Customer%20requested
```

### Update Order Status
```http
PATCH /api/v1/orders/{orderId}/status?status=SHIPPED
```

## Order State Machine

Valid status transitions:

```
PENDING_PAYMENT
  ├─> PAYMENT_AUTHORIZED ─> INVENTORY_RESERVED ─> CONFIRMED ─> SHIPPED ─> DELIVERED ─> REFUNDED
  ├─> PAYMENT_FAILED ─> CANCELLED
  └─> CANCELLED

Cancellable states: PENDING_PAYMENT, PAYMENT_AUTHORIZED, INVENTORY_RESERVED, CONFIRMED
Terminal states: CANCELLED, REFUNDED
```

## Events Published

The service publishes the following events to the `order-events` Kafka topic:

- **OrderCreated** - When order is created
- **OrderConfirmed** - When order is confirmed (payment + inventory)
- **OrderCancelled** - When order is cancelled
- **OrderStatusChanged** - On any status transition
- **OrderPaymentFailed** - When payment authorization fails

## Running the Service

### Using Maven

```bash
# Clean and build
mvn clean install

# Run the application
mvn spring-boot:run
```

### Using JAR

```bash
# Build JAR
mvn clean package

# Run JAR
java -jar target/order-service-1.0.0-SNAPSHOT.jar
```

## API Documentation

Once the service is running, access:

- **Swagger UI**: http://localhost:8086/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8086/api-docs

## Development

### Project Structure

```
order-service/
├── src/main/java/com/marketplace/order/
│   ├── aspect/              # AOP logging
│   ├── client/              # Service clients (Cart, User)
│   ├── config/              # Configuration classes
│   ├── controller/          # REST controllers
│   ├── domain/
│   │   ├── model/          # JPA entities
│   │   └── repository/     # Spring Data repositories
│   ├── dto/                # DTOs
│   ├── exception/          # Custom exceptions
│   ├── filter/             # Servlet filters
│   └── service/            # Business logic
│       ├── OrderService.java           # Main orchestration
│       ├── PaymentService.java         # Mock payment
│       ├── InventoryService.java       # Mock inventory
│       ├── OrderStateMachine.java      # State validation
│       └── OrderEventPublisher.java    # Kafka publishing
└── src/main/resources/
    ├── db/migration/       # Flyway migrations
    ├── application.yml     # Configuration
    └── logback-spring.xml  # Logging config
```

### Testing

```bash
# Run tests
mvn test

# Run with coverage
mvn test jacoco:report
```

## Monitoring

### Health Check

```bash
curl http://localhost:8086/actuator/health
```

### Metrics

The service exposes metrics for:
- Order creation rate
- Payment success/failure rate
- Inventory reservation rate
- Event publishing rate

### Logging

Logs are output in JSON format with correlation IDs:
- Console: JSON formatted
- File: `logs/order-service.log` with daily rotation

## Circuit Breakers

Circuit breakers are configured for external service calls:

- **Cart Service**: 50% failure threshold, 30s open duration
- **User Service**: 50% failure threshold, 30s open duration

## Known Limitations

1. **Mock Services**: Payment and Inventory services are mocked (not production-ready)
2. **Tax Calculation**: Currently set to $0.00 (not implemented)
3. **No Authentication**: Service endpoints are not secured
4. **Single Currency**: Only USD is supported
5. **No Shipping Tracking**: Shipping status is manual

## Future Enhancements

- [ ] Real payment gateway integration (Stripe, PayPal)
- [ ] Real inventory management system integration
- [ ] Tax calculation service integration
- [ ] Multi-currency support
- [ ] Shipping carrier integration
- [ ] Order refund workflow
- [ ] Scheduled jobs for expired reservation cleanup
- [ ] Order notification service integration

## Troubleshooting

### Database Connection Issues

```bash
# Test PostgreSQL connection
psql -h localhost -p 5466 -U order_user -d order_db
```

### Kafka Connection Issues

```bash
# Check Kafka broker
kafka-broker-api-versions.sh --bootstrap-server localhost:9092
```

### Service Communication Issues

```bash
# Check Cart Service
curl http://localhost:8083/actuator/health

# Check User Service
curl http://localhost:8081/actuator/health
```

## License

Copyright © 2024 Digital Marketplace. All rights reserved.
