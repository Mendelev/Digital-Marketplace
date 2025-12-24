# Inventory Service API Testing Guide

This guide provides manual testing steps for the Inventory Service. It covers admin stock management, public availability checks, and reservation flows.

## Prerequisites

1. **Start PostgreSQL for Inventory Service:**
   ```bash
   # If using shared Postgres
   docker-compose -f /Users/yuri.camargo/DevPro/Practice_projects/Digital-Marketplace/docker-compose.postgres.yml up -d

   # If using the service-local Postgres
   cd /Users/yuri.camargo/DevPro/Practice_projects/Digital-Marketplace/inventory-service
   docker-compose up -d
   ```

2. **Run database migrations:**
   ```bash
   cd /Users/yuri.camargo/DevPro/Practice_projects/Digital-Marketplace/inventory-service
   mvn flyway:migrate
   ```
   If you are using the shared Postgres on port 5432, run:
   ```bash
   mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/inventory_db -Dflyway.user=inventory_user -Dflyway.password=inventory_pass
   ```

3. **Start the Inventory Service:**
   ```bash
   # Option 1: Run locally with Maven
   mvn spring-boot:run

   # Option 2: Run in Docker container
   docker-compose -f docker-compose-full.yml up -d
   ```

4. **Verify Inventory Service is running:**
   ```bash
   # Docker compose maps to host port 8088 by default
   curl http://localhost:8088/api-docs
   ```

---

## Environment Variables (Optional)

```bash
# If running locally (non-docker), use http://localhost:8083
export INVENTORY_BASE_URL="http://localhost:8088"
export SKU="SKU-PS5"
export PRODUCT_ID="20787d17-0db8-4bfc-9c3d-4f8068646ecd"
export ORDER_ID="11111111-1111-1111-1111-111111111111"
export RESERVATION_ID="<replace-with-reservation-id>"
```

---

## Admin Stock Tests

### Test 1: Create Stock Item

```bash
curl -X POST "$INVENTORY_BASE_URL/api/v1/inventory/admin/stock-items" \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "'"$SKU"'",
    "productId": "'"$PRODUCT_ID"'",
    "initialQty": 25,
    "lowStockThreshold": 5
  }'
```

---

### Test 2: Adjust Stock Quantity

```bash
curl -X PUT "$INVENTORY_BASE_URL/api/v1/inventory/admin/stock-items/$SKU" \
  -H "Content-Type: application/json" \
  -d '{
    "availableQtyDelta": -3,
    "reason": "Manual adjustment"
  }'
```

---

### Test 3: Get Stock Item

```bash
curl "$INVENTORY_BASE_URL/api/v1/inventory/admin/stock-items/$SKU"
```

---

### Test 4: List Stock Items

```bash
curl "$INVENTORY_BASE_URL/api/v1/inventory/admin/stock-items?page=0&size=20"
```

---

### Test 5: Low Stock Items

```bash
curl "$INVENTORY_BASE_URL/api/v1/inventory/admin/low-stock?page=0&size=20"
```

---

## Public Availability Tests

### Test 6: Check Availability

```bash
curl "$INVENTORY_BASE_URL/api/v1/inventory/public/stock/$SKU"
```

---

### Test 7: Bulk Availability

```bash
curl -X POST "$INVENTORY_BASE_URL/api/v1/inventory/public/stock/bulk" \
  -H "Content-Type: application/json" \
  -d '["'"$SKU"'", "SKU-NOT-EXIST"]'
```

---

## Reservation Flow Tests (Service-to-Service)

Note: The service does not currently enforce `X-Service-Secret` on these endpoints.

### Test 8: Reserve Stock

```bash
curl -X POST "$INVENTORY_BASE_URL/api/v1/inventory/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "'"$ORDER_ID"'",
    "lines": [
      { "sku": "'"$SKU"'", "quantity": 2 }
    ]
  }'
```

Save the `reservationId` from the response.

---

### Test 9: Get Reservation

```bash
curl "$INVENTORY_BASE_URL/api/v1/inventory/reservations/$RESERVATION_ID"
```

---

### Test 10: Confirm Reservation

```bash
curl -X PUT "$INVENTORY_BASE_URL/api/v1/inventory/reservations/$RESERVATION_ID/confirm"
```

---

### Test 11: Release Reservation

```bash
curl -X PUT "$INVENTORY_BASE_URL/api/v1/inventory/reservations/$RESERVATION_ID/release?reason=Order%20cancelled"
```

---

## Notes

- Inventory runs on port 8083 inside the container; docker-compose maps it to 8088 on the host.
- If you want to validate product IDs against Catalog Service, ensure Catalog Service is running at `CATALOG_SERVICE_BASE_URL`.
