# Shipping Service

Shipment management and tracking service for the Digital Marketplace platform.

## Overview

The Shipping Service handles the complete shipment lifecycle from creation to delivery, including:
- Shipment creation from confirmed orders
- Automatic status progression simulation
- Real-time tracking history
- Event-driven integration with Order Service
- RESTful APIs for user and admin operations

## Features

- **Event-Driven Architecture**: Consumes `OrderConfirmed` events from Kafka to auto-create shipments
- **State Machine Management**: Strict validation of status transitions (PENDING → CREATED → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED)
- **Auto-Progression**: Background scheduler simulates realistic shipment progression
- **Tracking History**: Detailed tracking events with locations and timestamps
- **Flat Rate Shipping**: Configurable flat-rate shipping fee ($9.99 default)
- **JWT Authentication**: Secure user endpoints with role-based access control
- **Service-to-Service Auth**: Shared secret authentication for internal APIs
- **Event Sourcing**: All shipment events stored for audit trail
- **Circuit Breakers**: Resilience4j for fault-tolerant external service calls

## Architecture

### Technology Stack
- **Language**: Java 21
- **Framework**: Spring Boot 3.2.1
- **Database**: PostgreSQL (dedicated shipping_db)
- **Messaging**: Apache Kafka via Spring Cloud Stream
- **Security**: JWT (RS256), shared secrets
- **API Documentation**: OpenAPI 3.0 (Swagger UI)
- **Resilience**: Resilience4j circuit breakers
- **Containerization**: Docker

### Port
- **Service**: 8088

### Database
- **Name**: shipping_db
- **User**: shipping_user
- **Port**: 5434 (default local)

## Dependencies

### Internal Services
- **Auth Service** (port 8080): JWT public key validation
- **Order Service** (port 8086): Order event consumption

### External
- **PostgreSQL**: Database storage
- **Apache Kafka**: Event streaming (order-events → shipping-events)

## API Endpoints

### Service-to-Service (X-Service-Secret header required)

#### Create Shipment
```http
POST /api/v1/shipments
X-Service-Secret: <shared-secret>

{
  "orderId": "uuid",
  "userId": "uuid",
  "shippingAddress": {
    "street": "123 Main St",
    "city": "San Francisco",
    "state": "CA",
    "zip": "94105",
    "country": "USA"
  },
  "itemCount": 3
}
```

#### Cancel Shipment
```http
POST /api/v1/shipments/{id}/cancel
X-Service-Secret: <shared-secret>

{
  "reason": "Order cancelled"
}
```

#### Get Shipment by Order ID
```http
GET /api/v1/shipments/order/{orderId}
X-Service-Secret: <shared-secret>
```

### User Endpoints (JWT Bearer token required)

#### Get Shipment
```http
GET /api/v1/shipments/{id}
Authorization: Bearer <jwt-token>
```

#### Get Tracking History
```http
GET /api/v1/shipments/{id}/tracking
Authorization: Bearer <jwt-token>
```

#### Get My Shipments
```http
GET /api/v1/shipments/user/me
Authorization: Bearer <jwt-token>
```

### Admin Endpoints (JWT with ADMIN role required)

#### Update Shipment Status
```http
PATCH /api/v1/shipments/{id}/status
Authorization: Bearer <admin-jwt-token>

{
  "status": "DELIVERED",
  "reason": "Manual update"
}
```

#### List All Shipments
```http
GET /api/v1/shipments?status=IN_TRANSIT
Authorization: Bearer <admin-jwt-token>
```

## Shipment Status Lifecycle

```
PENDING → CREATED → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED
   ↓         ↓
CANCELLED  CANCELLED

Any status → RETURNED (admin only)
```

### Status Descriptions
- **PENDING**: Initial state, waiting to be processed
- **CREATED**: Shipping label generated, tracking number assigned
- **IN_TRANSIT**: Package picked up by carrier and in transit
- **OUT_FOR_DELIVERY**: Package out for final delivery
- **DELIVERED**: Successfully delivered to recipient
- **CANCELLED**: Shipment cancelled (only from PENDING/CREATED)
- **RETURNED**: Returned to sender (admin override)

## Event Integration

### Consumed Events
**Topic**: `order-events`

```json
{
  "eventType": "OrderConfirmed",
  "orderId": "uuid",
  "payload": {
    "orderId": "uuid",
    "userId": "uuid",
    "shippingAddress": {...},
    "itemCount": 3
  }
}
```

### Produced Events
**Topic**: `shipping-events`

Event Types:
- `ShipmentCreated`: When shipment is created
- `ShipmentUpdated`: When status changes
- `ShipmentDelivered`: When shipment is delivered
- `ShipmentCancelled`: When shipment is cancelled

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | Service port | 8088 |
| `DB_PASSWORD` | Database password | shipping_pass |
| `KAFKA_BROKERS` | Kafka broker addresses | localhost:9092 |
| `AUTH_SERVICE_URL` | Auth service base URL | http://localhost:8080 |
| `AUTH_SERVICE_SHARED_SECRET` | Shared secret for auth service | dev-secret-change-in-production |
| `ORDER_SERVICE_URL` | Order service base URL | http://localhost:8086 |
| `ORDER_SERVICE_SHARED_SECRET` | Shared secret for order service | dev-secret-change-in-production |
| `SHIPPING_SERVICE_SHARED_SECRET` | Service's shared secret | dev-secret-change-in-production |

### Application Properties

**Simulation Settings**:
- `shipment-simulation.auto-progress-enabled`: Enable auto-progression (default: true)
- `shipment-simulation.created-to-in-transit-delay-seconds`: Delay for CREATED → IN_TRANSIT (default: 120s)
- `shipment-simulation.in-transit-to-out-for-delivery-delay-seconds`: Delay for IN_TRANSIT → OUT_FOR_DELIVERY (default: 180s)
- `shipment-simulation.out-for-delivery-to-delivered-delay-seconds`: Delay for OUT_FOR_DELIVERY → DELIVERED (default: 60s)
- `shipment-simulation.delivery-success-rate`: Probability of successful delivery (default: 0.98)

**Business Settings**:
- `shipping.flat-rate-fee`: Flat shipping rate (default: 9.99)
- `shipping.default-currency`: Default currency (default: USD)
- `shipping.estimated-delivery-days`: Estimated days for delivery (default: 5)

## Running Locally

### Prerequisites
- Java 21+
- PostgreSQL
- Apache Kafka
- Maven 3.9+

### Database Setup
```sql
CREATE DATABASE shipping_db;
CREATE USER shipping_user WITH PASSWORD 'shipping_pass';
GRANT ALL PRIVILEGES ON DATABASE shipping_db TO shipping_user;
```

### Build
```bash
cd shipping-service
mvn clean package
```

### Run
```bash
java -jar target/shipping-service-1.0.0-SNAPSHOT.jar
```

Or with Maven:
```bash
mvn spring-boot:run
```

## Running with Docker

### Build Image
```bash
docker-compose -f docker-compose-full.yml build
```

### Run Container
```bash
docker-compose -f docker-compose-full.yml up -d
```

### View Logs
```bash
docker-compose -f docker-compose-full.yml logs -f shipping-service
```

### Stop
```bash
docker-compose -f docker-compose-full.yml down
```

## API Documentation

Once running, access Swagger UI at:
```
http://localhost:8088/swagger-ui.html
```

OpenAPI spec available at:
```
http://localhost:8088/api-docs
```

## Health Check

Health check endpoint:
```
http://localhost:8088/api-docs
```

## Monitoring & Logging

### Logging
- **Format**: JSON (Logstash encoder)
- **Level**: DEBUG for com.marketplace.shipping
- **MDC Fields**: correlationId, shipmentId, orderId, userId, operation

### Correlation ID
All requests are tracked with `X-Correlation-ID` header for distributed tracing.

## Development

### Project Structure
```
shipping-service/
├── src/main/java/com/marketplace/shipping/
│   ├── ShippingServiceApplication.java
│   ├── domain/
│   │   ├── model/         # JPA entities
│   │   └── repository/    # Data access
│   ├── service/           # Business logic
│   ├── controller/        # REST endpoints
│   ├── dto/              # Request/Response DTOs
│   ├── exception/        # Custom exceptions
│   ├── config/           # Configuration
│   ├── security/         # JWT validation
│   ├── filter/           # HTTP filters
│   ├── consumer/         # Kafka consumers
│   ├── scheduler/        # Background jobs
│   └── client/           # External service clients
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/     # Flyway SQL scripts
└── pom.xml
```

### Database Migrations
Flyway migrations are automatically applied on startup:
- V1: Create shipments table
- V2: Create shipment_events table (event sourcing)
- V3: Create shipment_tracking table

## Troubleshooting

### Service Won't Start
- Check PostgreSQL is running and accessible
- Verify Kafka is running
- Check port 8088 is not in use
- Review logs for detailed error messages

### Shipments Not Auto-Progressing
- Verify `shipment-simulation.auto-progress-enabled=true`
- Check scheduler logs for errors
- Ensure sufficient time has passed based on delay configurations

### JWT Validation Failing
- Verify Auth Service is running and accessible
- Check JWT public key endpoint is reachable
- Confirm token is not expired

## License

Part of the Digital Marketplace platform.
