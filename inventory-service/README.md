# Inventory Service

Stock management and reservation service for the Digital Marketplace platform.

## Overview

The Inventory Service manages product stock levels, handles stock reservations for orders with TTL expiration, and provides real-time availability information. It integrates with the Catalog Service for product validation and the Order Service for order fulfillment.

## Features

- **Stock Management**: Track available and reserved quantities per SKU
- **Stock Reservations**: Reserve stock for orders with automatic TTL expiration (default: 15 minutes)
- **Concurrency Control**: Optimistic and pessimistic locking to prevent race conditions
- **Low Stock Alerts**: Automated monitoring and alerting for items below threshold
- **Audit Trail**: Complete history of all stock movements via `stock_movements` table
- **Circuit Breaker**: Resilience4j integration for Catalog Service calls

## Architecture

**Port**: 8083
**Database**: PostgreSQL (port 5465)
**Technology Stack**: Java 21, Spring Boot 3.2.1, PostgreSQL 16

### Domain Model

- **StockItem**: Manages inventory with `availableQty`, `reservedQty`, optimistic locking
- **Reservation**: Order stock reservations with status lifecycle (ACTIVE → CONFIRMED/RELEASED/EXPIRED)
- **ReservationLine**: Individual SKUs and quantities in a reservation
- **StockMovement**: Immutable audit log of all stock changes

## API Endpoints

### Admin APIs (`/api/v1/inventory/admin`)

- `POST /stock-items` - Create or update stock item
- `PUT /stock-items/{sku}` - Adjust stock quantity
- `GET /stock-items/{sku}` - Get stock item details
- `GET /stock-items` - List all stock items (paginated)
- `GET /low-stock` - Get items below threshold

### Service-to-Service APIs (`/api/v1/inventory`)

**Requires `X-Service-Secret` header**

- `POST /reservations` - Reserve stock for an order
- `PUT /reservations/{id}/confirm` - Confirm reservation after payment
- `PUT /reservations/{id}/release` - Release reservation on cancellation
- `GET /reservations/{id}` - Get reservation details

### Public APIs (`/api/v1/inventory/public`)

- `GET /stock/{sku}` - Check availability for one SKU
- `POST /stock/bulk` - Check availability for multiple SKUs

## Configuration

### Application Properties

```yaml
inventory-service:
  reservation-ttl-minutes: 15  # Reservation expiration time
  default-low-stock-threshold: 10  # Default alert threshold

catalog-service:
  base-url: http://localhost:8082
  shared-secret: change-me-in-production

service-auth:
  shared-secret: change-me-in-production  # For service-to-service auth
```

### Environment Variables

- `DB_USERNAME` - Database user (default: inventory_user)
- `DB_PASSWORD` - Database password (default: inventory_pass)
- `CATALOG_SERVICE_BASE_URL` - Catalog Service URL
- `CATALOG_SERVICE_SECRET` - Shared secret for Catalog Service
- `SERVICE_SHARED_SECRET` - Shared secret for service authentication
- `RESERVATION_TTL_MINUTES` - Reservation TTL (default: 15)

## Running the Service

### 1. Start the Database

```bash
cd inventory-service
docker-compose up -d
```

This starts PostgreSQL on port 5465.

### 2. Build the Service

```bash
mvn clean package
```

### 3. Run the Service

```bash
mvn spring-boot:run
```

Or run the JAR:

```bash
java -jar target/inventory-service-1.0.0-SNAPSHOT.jar
```

The service starts on **http://localhost:8083**

### 4. Access Swagger UI

Open http://localhost:8083/swagger-ui.html to explore and test the APIs.

## Docker

### Start PostgreSQL (Local)

```bash
cd /Users/yuri.camargo/DevPro/Practice_projects/Digital-Marketplace/inventory-service
docker-compose up -d
```

### Build and Run Inventory Service

```bash
cd /Users/yuri.camargo/DevPro/Practice_projects/Digital-Marketplace/inventory-service
docker-compose -f docker-compose-full.yml up -d --build
```

The container maps port 8083 inside the container to port 8088 on the host to avoid conflicts.

## Testing

Manual test steps live in `inventory-service/TESTING.md`.

## Database Schema

### Tables

- **stock_items**: Stock levels per SKU with optimistic locking
- **reservations**: Stock reservations linked to orders
- **reservation_lines**: Line items for each reservation
- **stock_movements**: Audit trail of all stock changes

### Flyway Migrations

- `V1__create_stock_items_table.sql` - Stock items table with indexes
- `V2__create_reservations_tables.sql` - Reservations and lines
- `V3__create_stock_movements_table.sql` - Audit trail

## Key Business Logic

### Stock Reservation Flow

1. **Order Service** calls `POST /api/v1/inventory/reservations`
2. Inventory Service:
   - Validates all SKUs exist
   - Uses pessimistic locking (`SELECT FOR UPDATE`) on stock items
   - Checks availability for all lines atomically
   - Creates reservation with TTL (default 15 minutes)
   - Decrements `availableQty`, increments `reservedQty`
   - Records movements for audit
   - Returns reservation ID and expiration time

3. **Payment Success**: Order Service calls `PUT /reservations/{id}/confirm`
   - Marks reservation CONFIRMED
   - Stock remains in reserved state (allocated to order)

4. **Payment Failure/Cancellation**: Order Service calls `PUT /reservations/{id}/release`
   - Returns stock to available pool
   - Marks reservation RELEASED

5. **TTL Expiration**: Scheduler automatically expires reservations
   - Runs every 1 minute
   - Releases stock back to available
   - Marks reservation EXPIRED

### Concurrency Handling

- **Optimistic Locking**: `@Version` field on StockItem prevents lost updates
- **Pessimistic Locking**: `findBySkuForUpdate()` uses `SELECT FOR UPDATE` during reservations
- **Conflict Response**: Returns HTTP 409 with retry message on OptimisticLockException

## Scheduled Jobs

### ReservationCleanupScheduler

- **Frequency**: Every 1 minute
- **Purpose**: Expire reservations past TTL and release stock
- **Batch Size**: 100 reservations per run

### LowStockAlertScheduler

- **Frequency**: Every 30 minutes
- **Purpose**: Monitor items below low stock threshold
- **Output**: Warning logs (production: would publish events)

## Integration with Other Services

### Catalog Service

- **Purpose**: Validate product IDs and SKUs
- **Pattern**: Circuit breaker with fallback
- **Config**: `catalog-service.base-url`

### Order Service

- **Calls from Order Service**: Reserve, confirm, release stock
- **Authentication**: `X-Service-Secret` header
- **Future**: Event-driven updates when reservations expire

## Monitoring & Observability

### Logging

- **Format**: Structured JSON (Logstash encoder)
- **MDC Context**: correlationId, userId, operation, sku, reservationId, orderId
- **Correlation**: `X-Correlation-ID` header propagated across services

### Health Checks

- **Endpoint**: `/actuator/health`
- **Metrics**: `/actuator/metrics`

## Error Handling

All errors return standardized `ErrorResponse`:

```json
{
  "status": 409,
  "error": "Insufficient Stock",
  "message": "Insufficient stock for SKU ABC123: requested 5, available 2",
  "path": "/api/v1/inventory/reservations",
  "timestamp": "2025-12-19T20:30:45.123Z"
}
```

### Common Status Codes

- **409 Conflict**: Insufficient stock, duplicate reservation, concurrent update
- **404 Not Found**: Invalid SKU or reservation not found
- **410 Gone**: Reservation expired
- **400 Bad Request**: Invalid reservation state
- **503 Service Unavailable**: Catalog Service down

## Testing

### Manual Testing with Swagger

1. Start the service
2. Open http://localhost:8083/swagger-ui.html
3. Use Admin APIs to create stock items
4. Test reservation flow:
   - Reserve stock
   - Confirm or release
   - Check stock availability

### Example: Create Stock Item

```bash
curl -X POST http://localhost:8083/api/v1/inventory/admin/stock-items \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "LAPTOP-001",
    "productId": "550e8400-e29b-41d4-a716-446655440000",
    "initialQty": 100,
    "lowStockThreshold": 10
  }'
```

### Example: Reserve Stock

```bash
curl -X POST http://localhost:8083/api/v1/inventory/reservations \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: change-me-in-production" \
  -d '{
    "orderId": "660e8400-e29b-41d4-a716-446655440000",
    "lines": [
      {
        "sku": "LAPTOP-001",
        "quantity": 2
      }
    ]
  }'
```

## Future Enhancements

- **Event Publishing**: Kafka integration for StockReserved, StockConfirmed, etc.
- **Notification Integration**: Low stock alerts via Notification Service
- **Redis Caching**: Cache frequently checked SKUs
- **Metrics**: Micrometer metrics for reservation success rates
- **Batch Operations**: Bulk stock adjustments
- **Stock Transfers**: Move stock between warehouses

## Security Considerations

- **Service Authentication**: All service-to-service calls require `X-Service-Secret`
- **Input Validation**: Jakarta Validation on all DTOs
- **SQL Injection**: JPA prevents SQL injection
- **Race Conditions**: Pessimistic locking prevents overselling

## Development

### Project Structure

```
inventory-service/
├── src/main/java/com/marketplace/inventory/
│   ├── InventoryServiceApplication.java
│   ├── client/              # External service clients
│   ├── config/              # Configuration classes
│   ├── controller/          # REST controllers
│   ├── domain/
│   │   ├── model/           # JPA entities
│   │   └── repository/      # Spring Data repositories
│   ├── dto/                 # Local DTOs
│   ├── exception/           # Custom exceptions
│   ├── filter/              # Servlet filters
│   ├── scheduler/           # Scheduled jobs
│   └── service/             # Business logic
└── src/main/resources/
    ├── application.yml
    ├── logback-spring.xml
    └── db/migration/        # Flyway migrations
```

### Adding New Features

1. Update domain model if needed
2. Create Flyway migration
3. Add DTOs in `common/shared-dtos` if used by other services
4. Implement business logic in service layer
5. Add controller endpoints
6. Update this README

## Troubleshooting

### Database Connection Issues

```bash
# Check if database is running
docker-compose ps

# View database logs
docker-compose logs inventory-db

# Restart database
docker-compose restart inventory-db
```

### Flyway Migration Failures

```bash
# Check migration status
mvn flyway:info

# Repair failed migration
mvn flyway:repair

# Baseline existing database
mvn flyway:baseline
```

### Circuit Breaker Issues

Check Catalog Service availability:
```bash
curl http://localhost:8082/actuator/health
```

## License

Part of the Digital Marketplace platform.
