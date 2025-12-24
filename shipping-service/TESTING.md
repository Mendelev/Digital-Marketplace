# Shipping Service - Testing Guide

This document provides comprehensive testing scenarios for the Shipping Service.

## Prerequisites

### Running Services
- **Shipping Service**: http://localhost:8088
- **Auth Service**: http://localhost:8080 (for JWT tokens)
- **Order Service**: http://localhost:8086 (for event integration)
- **PostgreSQL**: localhost:5432 (shipping_db)
- **Kafka**: localhost:9092

### Environment Setup

#### Option 1: Using Docker Compose (Recommended)
```bash
# Start everything (PostgreSQL + Shipping Service)
cd shipping-service
docker-compose up -d

# View logs
docker-compose logs -f shipping-service

# Stop
docker-compose down
```

#### Option 2: Manual PostgreSQL Setup
```bash
# If using existing PostgreSQL container, connect as postgres user:
docker exec -it marketplace-postgres psql -U postgres

# Inside psql, run:
CREATE DATABASE shipping_db;
CREATE USER shipping_user WITH PASSWORD 'shipping_pass';
GRANT ALL PRIVILEGES ON DATABASE shipping_db TO shipping_user;
\c shipping_db
GRANT ALL ON SCHEMA public TO shipping_user;
GRANT CREATE ON SCHEMA public TO shipping_user;
\q

# Or as one-liners from host:
docker exec -it marketplace-postgres psql -U postgres -c "CREATE DATABASE shipping_db;"
docker exec -it marketplace-postgres psql -U postgres -c "CREATE USER shipping_user WITH PASSWORD 'shipping_pass';"
docker exec -it marketplace-postgres psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE shipping_db TO shipping_user;"
docker exec -it marketplace-postgres psql -U postgres -d shipping_db -c "GRANT ALL ON SCHEMA public TO shipping_user;"
docker exec -it marketplace-postgres psql -U postgres -d shipping_db -c "GRANT CREATE ON SCHEMA public TO shipping_user;"

# Start service
cd shipping-service
mvn spring-boot:run
```

#### Option 3: Local PostgreSQL (non-Docker)
```bash
# Create database and user
createdb -U postgres shipping_db
psql -U postgres -c "CREATE USER shipping_user WITH PASSWORD 'shipping_pass';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE shipping_db TO shipping_user;"
psql -U postgres -d shipping_db -c "GRANT ALL ON SCHEMA public TO shipping_user;"
psql -U postgres -d shipping_db -c "GRANT CREATE ON SCHEMA public TO shipping_user;"

# Start service
cd shipping-service
mvn spring-boot:run
```

### Test Data
- Service Secret: `dev-secret-change-in-production`
- Test User ID: `550e8400-e29b-41d4-a716-446655440000`
- Test Order ID: `660e8400-e29b-41d4-a716-446655440001`

## API Testing Scenarios

### 1. Service-to-Service: Create Shipment

**Request:**
```bash
curl -X POST http://localhost:8088/api/v1/shipments \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: dev-secret-change-in-production" \
  -d '{
    "orderId": "660e8400-e29b-41d4-a716-446655440001",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "shippingAddress": {
      "label": "Home",
      "street": "123 Main St",
      "number": "Apt 4B",
      "city": "San Francisco",
      "state": "CA",
      "zip": "94105",
      "country": "USA",
      "complement": null
    },
    "itemCount": 3,
    "packageWeightKg": 2.5,
    "packageDimensions": "30x20x15 cm"
  }'
```

**Expected Response (201):**
```json
{
  "shipmentId": "uuid",
  "orderId": "660e8400-e29b-41d4-a716-446655440001",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "CREATED",
  "trackingNumber": "TRACK-ABC12345",
  "carrier": "MOCK",
  "shippingFee": 9.99,
  "currency": "USD",
  "itemCount": 3,
  "packageWeightKg": 2.5,
  "packageDimensions": "30x20x15 cm",
  "shippingAddress": {...},
  "estimatedDeliveryDate": "2025-12-29T...",
  "actualDeliveryDate": null,
  "shippedAt": null,
  "deliveredAt": null,
  "createdAt": "2025-12-24T...",
  "updatedAt": "2025-12-24T..."
}
```

**Validation:**
- Shipment created with CREATED status
- Tracking number generated (TRACK-XXXXXXXX format)
- Estimated delivery date is 5 days from now
- Initial tracking event created
- ShipmentCreated event published to Kafka

---

### 2. Service-to-Service: Get Shipment by Order ID

**Request:**
```bash
curl -X GET http://localhost:8088/api/v1/shipments/order/660e8400-e29b-41d4-a716-446655440001 \
  -H "X-Service-Secret: dev-secret-change-in-production"
```

**Expected Response (200):**
```json
{
  "shipmentId": "uuid",
  "orderId": "660e8400-e29b-41d4-a716-446655440001",
  "status": "CREATED",
  ...
}
```

**Error Cases:**
```bash
# Invalid service secret (403)
curl -X GET http://localhost:8088/api/v1/shipments/order/660e8400-e29b-41d4-a716-446655440001 \
  -H "X-Service-Secret: wrong-secret"

# Shipment not found (404)
curl -X GET http://localhost:8088/api/v1/shipments/order/99999999-9999-9999-9999-999999999999 \
  -H "X-Service-Secret: dev-secret-change-in-production"
```

---

### 3. User Endpoint: Get Shipment with JWT

**Step 1: Get JWT Token from Auth Service**
```bash
# Login
JWT_TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@example.com",
    "password": "password123"
  }' | jq -r '.accessToken')
```

**Step 2: Get Shipment**
```bash
curl -X GET http://localhost:8088/api/v1/shipments/{shipmentId} \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Response (200):**
Shipment details (only if user owns the shipment or is admin)

**Error Cases:**
```bash
# No auth token (401)
curl -X GET http://localhost:8088/api/v1/shipments/{shipmentId}

# Access denied - different user (403)
# (Use JWT token from different user)

# Shipment not found (404)
curl -X GET http://localhost:8088/api/v1/shipments/99999999-9999-9999-9999-999999999999 \
  -H "Authorization: Bearer $JWT_TOKEN"
```

---

### 4. User Endpoint: Get Tracking History

**Request:**
```bash
curl -X GET http://localhost:8088/api/v1/shipments/{shipmentId}/tracking \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Response (200):**
```json
[
  {
    "id": 1,
    "status": "CREATED",
    "location": "Distribution Center - Label created",
    "description": "Shipping label generated, package ready for pickup",
    "eventTime": "2025-12-24T10:00:00Z",
    "createdAt": "2025-12-24T10:00:00Z"
  }
]
```

**Validation:**
- Events ordered by most recent first
- Each event has status, location, description, and timestamp

---

### 5. User Endpoint: Get My Shipments

**Request:**
```bash
curl -X GET http://localhost:8088/api/v1/shipments/user/me \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Response (200):**
```json
[
  {
    "shipmentId": "uuid1",
    "orderId": "order-uuid1",
    "status": "DELIVERED",
    ...
  },
  {
    "shipmentId": "uuid2",
    "orderId": "order-uuid2",
    "status": "IN_TRANSIT",
    ...
  }
]
```

**Validation:**
- Returns all shipments for authenticated user
- Empty array if no shipments

---

### 6. Admin: Update Shipment Status

**Step 1: Get Admin JWT Token**
```bash
ADMIN_TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "admin123"
  }' | jq -r '.accessToken')
```

**Step 2: Update Status**
```bash
curl -X PATCH http://localhost:8088/api/v1/shipments/{shipmentId}/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "DELIVERED",
    "reason": "Manual delivery confirmation"
  }'
```

**Expected Response (200):**
Updated shipment with new status

**Error Cases:**
```bash
# Non-admin user (403)
curl -X PATCH http://localhost:8088/api/v1/shipments/{shipmentId}/status \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "DELIVERED"}'

# Invalid status (400)
curl -X PATCH http://localhost:8088/api/v1/shipments/{shipmentId}/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "INVALID_STATUS"}'

# Invalid transition (400)
# Try to mark DELIVERED shipment as CREATED
```

---

### 7. Admin: List All Shipments

**Request:**
```bash
# All shipments
curl -X GET http://localhost:8088/api/v1/shipments \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Filter by status
curl -X GET "http://localhost:8088/api/v1/shipments?status=IN_TRANSIT" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Expected Response (200):**
Array of all shipments (or filtered by status)

---

### 8. Service-to-Service: Cancel Shipment

**Request:**
```bash
curl -X POST http://localhost:8088/api/v1/shipments/{shipmentId}/cancel \
  -H "X-Service-Secret: dev-secret-change-in-production" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Order cancelled by customer"
  }'
```

**Expected Response (200):**
Shipment with CANCELLED status

**Error Cases:**
```bash
# Cannot cancel shipped shipment (400)
# Try to cancel a shipment with status IN_TRANSIT or later
```

---

## Event-Driven Testing

### Test OrderConfirmed Event Processing

**Step 1: Publish OrderConfirmed Event to Kafka**

Using Kafka console producer:
```bash
kafka-console-producer --broker-list localhost:9092 --topic order-events

# Paste this JSON:
{
  "eventId": "770e8400-e29b-41d4-a716-446655440001",
  "eventType": "OrderConfirmed",
  "orderId": "880e8400-e29b-41d4-a716-446655440002",
  "sequenceNumber": 1,
  "payload": {
    "orderId": "880e8400-e29b-41d4-a716-446655440002",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "shippingAddress": {
      "label": "Home",
      "street": "456 Oak Ave",
      "number": "Suite 200",
      "city": "New York",
      "state": "NY",
      "zip": "10001",
      "country": "USA",
      "complement": null
    },
    "itemCount": 2,
    "totalAmount": "99.99"
  },
  "publishedAt": "2025-12-24T10:00:00Z"
}
```

**Step 2: Verify Shipment Created**
```bash
curl -X GET http://localhost:8088/api/v1/shipments/order/880e8400-e29b-41d4-a716-446655440002 \
  -H "X-Service-Secret: dev-secret-change-in-production"
```

**Validation:**
- Shipment automatically created from event
- ShipmentCreated event published to shipping-events topic
- Tracking event created

**Step 3: Verify Idempotency**
Publish the same event again - shipment should not be duplicated.

---

### Test ShipmentCreated Event Published

**Monitor shipping-events topic:**
```bash
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic shipping-events --from-beginning
```

**Expected Event:**
```json
{
  "eventId": "uuid",
  "eventType": "ShipmentCreated",
  "shipmentId": "uuid",
  "sequenceNumber": 1,
  "payload": {
    "shipmentId": "uuid",
    "orderId": "uuid",
    "userId": "uuid",
    "status": "CREATED",
    "trackingNumber": "TRACK-XXXXXXXX",
    "carrier": "MOCK",
    "shippingFee": "9.99",
    "currency": "USD",
    "estimatedDeliveryDate": "...",
    "createdAt": "..."
  },
  "publishedAt": "..."
}
```

---

## Auto-Progression Testing

### Test Automatic Status Progression

**Step 1: Create Shipment**
Create a shipment using the API or event.

**Step 2: Monitor Auto-Progression**
Watch logs for scheduler activity:
```bash
tail -f logs/shipping-service.log | grep "ShipmentProgressScheduler"
```

**Expected Behavior:**
1. After ~120s: CREATED → IN_TRANSIT
2. After ~180s: IN_TRANSIT → OUT_FOR_DELIVERY
3. After ~60s: OUT_FOR_DELIVERY → DELIVERED

**Step 3: Verify Events Published**
Check shipping-events topic for ShipmentUpdated and ShipmentDelivered events.

**Step 4: Verify Tracking History**
```bash
curl -X GET http://localhost:8088/api/v1/shipments/{shipmentId}/tracking \
  -H "Authorization: Bearer $JWT_TOKEN"
```

Should show multiple tracking events at different locations.

---

## State Machine Testing

### Valid Transitions
Test all valid status transitions:

1. **PENDING → CREATED** (automatic on creation)
2. **CREATED → IN_TRANSIT**
3. **IN_TRANSIT → OUT_FOR_DELIVERY**
4. **OUT_FOR_DELIVERY → DELIVERED**
5. **PENDING → CANCELLED**
6. **CREATED → CANCELLED**

### Invalid Transitions
Test that invalid transitions are rejected:

```bash
# Try to mark IN_TRANSIT as CREATED (should fail with 400)
curl -X PATCH http://localhost:8088/api/v1/shipments/{shipmentId}/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "CREATED"}'

# Try to cancel IN_TRANSIT shipment (should fail with 400)
curl -X POST http://localhost:8088/api/v1/shipments/{in-transit-shipment-id}/cancel \
  -H "X-Service-Secret: dev-secret-change-in-production"
```

---

## Database Verification

### Check Shipments Table
```sql
SELECT shipment_id, order_id, status, tracking_number, created_at, updated_at
FROM shipments
ORDER BY created_at DESC
LIMIT 10;
```

### Check Shipment Events (Event Sourcing)
```sql
SELECT event_id, shipment_id, event_type, sequence_number, published_at
FROM shipment_events
ORDER BY sequence_number DESC
LIMIT 10;
```

### Check Tracking History
```sql
SELECT shipment_id, status, location, description, event_time
FROM shipment_tracking
WHERE shipment_id = 'your-shipment-id'
ORDER BY event_time DESC;
```

---

## Performance Testing

### Load Test: Create Multiple Shipments
```bash
for i in {1..100}; do
  ORDER_ID=$(uuidgen)
  curl -X POST http://localhost:8088/api/v1/shipments \
    -H "Content-Type: application/json" \
    -H "X-Service-Secret: dev-secret-change-in-production" \
    -d "{
      \"orderId\": \"$ORDER_ID\",
      \"userId\": \"550e8400-e29b-41d4-a716-446655440000\",
      \"shippingAddress\": {
        \"street\": \"123 Main St\",
        \"city\": \"SF\",
        \"state\": \"CA\",
        \"zip\": \"94105\",
        \"country\": \"USA\"
      },
      \"itemCount\": 1
    }" &
done
wait
```

**Validation:**
- All shipments created successfully
- No duplicate tracking numbers
- Events published to Kafka
- Database constraints enforced

---

## Troubleshooting

### Shipment Not Auto-Progressing
1. Check scheduler is enabled: `shipment-simulation.auto-progress-enabled=true`
2. Verify delays have elapsed
3. Check logs for scheduler errors
4. Confirm shipment status is progressable (CREATED, IN_TRANSIT, OUT_FOR_DELIVERY)

### Events Not Published
1. Verify Kafka is running
2. Check Kafka broker configuration
3. Review logs for Kafka errors
4. Confirm topic exists: `kafka-topics --list --bootstrap-server localhost:9092`

### JWT Validation Failing
1. Verify Auth Service is running
2. Check public key endpoint is accessible
3. Ensure token is not expired
4. Confirm roles are correct in JWT

---

## Integration Test Checklist

- [ ] Create shipment via API
- [ ] Create shipment via OrderConfirmed event
- [ ] Get shipment with JWT (owner)
- [ ] Get shipment denied (non-owner)
- [ ] Get tracking history
- [ ] Get user's shipments
- [ ] Update status (admin)
- [ ] Cancel shipment (service-to-service)
- [ ] Cannot cancel shipped shipment
- [ ] List all shipments (admin)
- [ ] Filter shipments by status
- [ ] Auto-progression works
- [ ] Events published to Kafka
- [ ] Idempotency works
- [ ] State machine validates transitions
- [ ] Correlation IDs propagate
- [ ] Error responses include correlationId

---

## API Documentation

Swagger UI: http://localhost:8088/swagger-ui.html
OpenAPI Spec: http://localhost:8088/api-docs
