# Cart Service - Digital Marketplace

Shopping cart microservice for the Digital Marketplace platform.

## Overview

The Cart Service manages shopping carts for customers, validates products via the Catalog Service, and provides a complete cart lifecycle from item addition through checkout.

**Port:** 8083
**Database:** PostgreSQL (localhost:5465)
**External Dependencies:** Catalog Service (localhost:8082)

## Features

- ✅ Full CRUD operations for cart management
- ✅ Product validation via Catalog Service integration
- ✅ Price snapshots (captures price at time of adding to cart)
- ✅ Circuit breaker for resilient Catalog Service calls
- ✅ Correlation ID propagation for distributed tracing
- ✅ OpenAPI/Swagger documentation
- ✅ Structured JSON logging
- ✅ Global exception handling with standardized error responses

---

## Getting Started

### Prerequisites

- Java 21
- Maven 3.6+
- PostgreSQL 14+
- Catalog Service running on port 8082 (for product validation)

### Database Setup

Create the PostgreSQL database and user:

```bash
# Connect to PostgreSQL
psql -U postgres

# Create database and user
CREATE DATABASE cart_db;
CREATE USER cart_user WITH PASSWORD 'cart_pass';
GRANT ALL PRIVILEGES ON DATABASE cart_db TO cart_user;

# Grant schema permissions (if needed)
\c cart_db
GRANT ALL ON SCHEMA public TO cart_user;

# Exit
\q
```

### Build and Run

```bash
# Navigate to cart-service directory
cd cart-service

# Clean and install dependencies
mvn clean install

# Run the application
mvn spring-boot:run
```

The service will start on **http://localhost:8083**

### Verify Service is Running

```bash
# Health check
curl http://localhost:8083/actuator/health

# Expected response:
# {"status":"UP"}
```

---

## API Documentation

### Swagger UI (Interactive Documentation)

Access the interactive API documentation at:

**http://localhost:8083/swagger-ui.html**

The Swagger UI allows you to:
- Browse all available endpoints
- View request/response schemas
- Test API calls directly from the browser
- See example values

### OpenAPI JSON Specification

**http://localhost:8083/api-docs**

---

## API Endpoints

### Base URL
```
http://localhost:8083/api/v1/carts
```

### Endpoints Summary

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/{userId}` | Get or create active cart | None (MVP) |
| POST | `/{userId}/items` | Add item to cart | None (MVP) |
| PUT | `/{userId}/items/{cartItemId}` | Update item quantity | None (MVP) |
| DELETE | `/{userId}/items/{cartItemId}` | Remove item from cart | None (MVP) |
| DELETE | `/{userId}` | Clear all items from cart | None (MVP) |
| POST | `/{userId}/checkout` | Checkout cart | None (MVP) |

**Note:** Authentication is not implemented in this MVP. Use any valid UUID for `userId`.

---

## Testing the API

### Using cURL

#### 1. Get or Create Cart

```bash
# Replace {userId} with a valid UUID
curl -X GET http://localhost:8083/api/v1/carts/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" | jq
```

**Expected Response:**
```json
{
  "cartId": "7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ACTIVE",
  "currency": "USD",
  "items": [],
  "itemCount": 0,
  "subtotal": "0.00",
  "createdAt": "2025-12-18T10:30:00",
  "updatedAt": "2025-12-18T10:30:00"
}
```

#### 2. Add Item to Cart

**Important:** Ensure the Catalog Service is running and has products available.

```bash
# Add a product to cart
# Replace productId with a valid product from Catalog Service
curl -X POST http://localhost:8083/api/v1/carts/550e8400-e29b-41d4-a716-446655440000/items \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "quantity": 2
  }' | jq
```

**Expected Response:**
```json
{
  "cartId": "7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ACTIVE",
  "currency": "USD",
  "items": [
    {
      "cartItemId": "1a2b3c4d-5e6f-7890-abcd-ef1234567890",
      "productId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "sku": "PROD-A1B2C3D4",
      "titleSnapshot": "Premium Wireless Headphones",
      "unitPriceSnapshot": "149.99",
      "currency": "USD",
      "quantity": 2,
      "subtotal": "299.98"
    }
  ],
  "itemCount": 2,
  "subtotal": "299.98",
  "createdAt": "2025-12-18T10:30:00",
  "updatedAt": "2025-12-18T10:35:00"
}
```

**Error Scenarios:**

**Product Not Found (404):**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Product not found: a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "path": "/api/v1/carts/550e8400-e29b-41d4-a716-446655440000/items",
  "timestamp": "2025-12-18T10:35:00"
}
```

**Product Not Active (422):**
```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Product is not available for purchase: a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "path": "/api/v1/carts/550e8400-e29b-41d4-a716-446655440000/items",
  "timestamp": "2025-12-18T10:35:00"
}
```

**Catalog Service Unavailable (503):**
```json
{
  "status": 503,
  "error": "Service Unavailable",
  "message": "Product catalog is temporarily unavailable",
  "path": "/api/v1/carts/550e8400-e29b-41d4-a716-446655440000/items",
  "timestamp": "2025-12-18T10:35:00"
}
```

#### 3. Update Item Quantity

```bash
# Update quantity to 5
# Replace {cartItemId} with the ID from add item response
curl -X PUT http://localhost:8083/api/v1/carts/550e8400-e29b-41d4-a716-446655440000/items/1a2b3c4d-5e6f-7890-abcd-ef1234567890 \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 5
  }' | jq
```

**Set quantity to 0 to remove item:**
```bash
curl -X PUT http://localhost:8083/api/v1/carts/550e8400-e29b-41d4-a716-446655440000/items/1a2b3c4d-5e6f-7890-abcd-ef1234567890 \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 0
  }' | jq
```

#### 4. Remove Item from Cart

```bash
# Remove specific item
curl -X DELETE http://localhost:8083/api/v1/carts/550e8400-e29b-41d4-a716-446655440000/items/1a2b3c4d-5e6f-7890-abcd-ef1234567890

# Expected: 204 No Content (empty response)
```

#### 5. Clear Cart

```bash
# Remove all items from cart
curl -X DELETE http://localhost:8083/api/v1/carts/550e8400-e29b-41d4-a716-446655440000

# Expected: 204 No Content (empty response)
```

#### 6. Checkout Cart

```bash
# Checkout the cart (marks as CHECKED_OUT and creates new ACTIVE cart)
curl -X POST http://localhost:8083/api/v1/carts/550e8400-e29b-41d4-a716-446655440000/checkout \
  -H "Content-Type: application/json" | jq
```

**Expected Response:**
```json
{
  "cart": {
    "cartId": "7a8b9c0d-1e2f-3a4b-5c6d-7e8f9a0b1c2d",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "CHECKED_OUT",
    "currency": "USD",
    "items": [
      {
        "cartItemId": "1a2b3c4d-5e6f-7890-abcd-ef1234567890",
        "productId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "sku": "PROD-A1B2C3D4",
        "titleSnapshot": "Premium Wireless Headphones",
        "unitPriceSnapshot": "149.99",
        "currency": "USD",
        "quantity": 2,
        "subtotal": "299.98"
      }
    ],
    "itemCount": 2,
    "subtotal": "299.98",
    "createdAt": "2025-12-18T10:30:00",
    "updatedAt": "2025-12-18T10:40:00"
  },
  "message": "Cart checked out successfully. A new cart has been created."
}
```

**Error - Empty Cart (400):**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot checkout empty cart",
  "path": "/api/v1/carts/550e8400-e29b-41d4-a716-446655440000/checkout",
  "timestamp": "2025-12-18T10:40:00"
}
```

---

### Using Postman

#### Setup

1. **Import Collection:**
   - Create a new Collection named "Cart Service"
   - Set base URL variable: `{{baseUrl}}` = `http://localhost:8083`
   - Set user ID variable: `{{userId}}` = `550e8400-e29b-41d4-a716-446655440000`

2. **Add Requests:**

**Get Cart**
- Method: `GET`
- URL: `{{baseUrl}}/api/v1/carts/{{userId}}`

**Add Item to Cart**
- Method: `POST`
- URL: `{{baseUrl}}/api/v1/carts/{{userId}}/items`
- Body (raw JSON):
```json
{
  "productId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "quantity": 2
}
```

**Update Item Quantity**
- Method: `PUT`
- URL: `{{baseUrl}}/api/v1/carts/{{userId}}/items/{{cartItemId}}`
- Body (raw JSON):
```json
{
  "quantity": 5
}
```

**Remove Item**
- Method: `DELETE`
- URL: `{{baseUrl}}/api/v1/carts/{{userId}}/items/{{cartItemId}}`

**Clear Cart**
- Method: `DELETE`
- URL: `{{baseUrl}}/api/v1/carts/{{userId}}`

**Checkout**
- Method: `POST`
- URL: `{{baseUrl}}/api/v1/carts/{{userId}}/checkout`

---

## Testing Tips

### 1. End-to-End Testing Flow

Test the complete cart lifecycle:

```bash
# Step 1: Get empty cart (creates new cart)
curl -X GET http://localhost:8083/api/v1/carts/550e8400-e29b-41d4-a716-446655440000 | jq

# Step 2: Add first product
curl -X POST http://localhost:8083/api/v1/carts/550e8400-e29b-41d4-a716-446655440000/items \
  -H "Content-Type: application/json" \
  -d '{"productId": "PRODUCT_ID_1", "quantity": 2}' | jq

# Step 3: Add second product
curl -X POST http://localhost:8083/api/v1/carts/550e8400-e29b-41d4-a716-446655440000/items \
  -H "Content-Type: application/json" \
  -d '{"productId": "PRODUCT_ID_2", "quantity": 1}' | jq

# Step 4: Add same product again (should increment quantity)
curl -X POST http://localhost:8083/api/v1/carts/550e8400-e29b-41d4-a716-446655440000/items \
  -H "Content-Type: application/json" \
  -d '{"productId": "PRODUCT_ID_1", "quantity": 3}' | jq
# Notice: quantity should now be 5 for PRODUCT_ID_1

# Step 5: Update quantity
curl -X PUT http://localhost:8083/api/v1/carts/550e8400-e29b-41d4-a716-446655440000/items/CART_ITEM_ID \
  -H "Content-Type: application/json" \
  -d '{"quantity": 10}' | jq

# Step 6: Get cart to verify changes
curl -X GET http://localhost:8083/api/v1/carts/550e8400-e29b-41d4-a716-446655440000 | jq

# Step 7: Checkout
curl -X POST http://localhost:8083/api/v1/carts/550e8400-e29b-41d4-a716-446655440000/checkout | jq

# Step 8: Get cart again (should be new empty cart)
curl -X GET http://localhost:8083/api/v1/carts/550e8400-e29b-41d4-a716-446655440000 | jq
```

### 2. Testing Circuit Breaker

Test resilience when Catalog Service is unavailable:

```bash
# Step 1: Stop Catalog Service (if running)

# Step 2: Try to add item to cart
curl -X POST http://localhost:8083/api/v1/carts/550e8400-e29b-41d4-a716-446655440000/items \
  -H "Content-Type: application/json" \
  -d '{"productId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890", "quantity": 1}' | jq

# Expected: 503 Service Unavailable

# Step 3: Check circuit breaker health
curl http://localhost:8083/actuator/health | jq

# Step 4: Restart Catalog Service and try again
```

### 3. Testing Correlation IDs

Verify distributed tracing:

```bash
# Send request with custom correlation ID
curl -X GET http://localhost:8083/api/v1/carts/550e8400-e29b-41d4-a716-446655440000 \
  -H "X-Correlation-ID: my-test-correlation-123" \
  -v

# Check response headers for correlation ID
# Look for: X-Correlation-ID: my-test-correlation-123

# Check logs - should contain the correlation ID
tail -f logs/cart-service.log | grep "my-test-correlation-123"
```

### 4. Testing Validation

Test request validation:

```bash
# Missing productId
curl -X POST http://localhost:8083/api/v1/carts/550e8400-e29b-41d4-a716-446655440000/items \
  -H "Content-Type: application/json" \
  -d '{"quantity": 1}' | jq
# Expected: 400 Bad Request - "Product ID is required"

# Invalid quantity (0 or negative)
curl -X POST http://localhost:8083/api/v1/carts/550e8400-e29b-41d4-a716-446655440000/items \
  -H "Content-Type: application/json" \
  -d '{"productId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890", "quantity": 0}' | jq
# Expected: 400 Bad Request - "Quantity must be at least 1"

# Invalid UUID format
curl -X GET http://localhost:8083/api/v1/carts/invalid-uuid | jq
# Expected: 400 Bad Request
```

### 5. Database Verification

Connect to PostgreSQL to verify data:

```bash
# Connect to database
psql -U cart_user -d cart_db

# View all carts
SELECT * FROM cart;

# View cart items
SELECT * FROM cart_item;

# View cart with items
SELECT c.cart_id, c.user_id, c.status, ci.title_snapshot, ci.quantity, ci.unit_price_snapshot
FROM cart c
LEFT JOIN cart_item ci ON c.cart_id = ci.cart_id
WHERE c.user_id = '550e8400-e29b-41d4-a716-446655440000';

# Check Flyway migrations
SELECT * FROM flyway_schema_history;
```

---

## Common Issues and Solutions

### Issue: Database Connection Failed

**Error:**
```
Connection to localhost:5465 refused
```

**Solution:**
1. Verify PostgreSQL is running: `pg_isready`
2. Check PostgreSQL is listening on port 5465
3. Verify database and user exist
4. Check credentials in `application.yml`

### Issue: Catalog Service Unavailable

**Error:**
```
503 Service Unavailable - Product catalog is temporarily unavailable
```

**Solution:**
1. Start the Catalog Service on port 8082
2. Verify Catalog Service health: `curl http://localhost:8082/actuator/health`
3. Check circuit breaker status: `curl http://localhost:8083/actuator/health`

### Issue: Product Not Found

**Error:**
```
404 Not Found - Product not found: {productId}
```

**Solution:**
1. Verify the product exists in Catalog Service
2. Use Catalog Service Swagger UI to find valid product IDs: http://localhost:8082/swagger-ui.html
3. Ensure product status is "ACTIVE"

### Issue: Maven Build Fails

**Error:**
```
Cannot resolve dependency: shared-dtos
```

**Solution:**
1. Build shared-dtos first:
   ```bash
   cd ../common/shared-dtos
   mvn clean install
   cd ../../cart-service
   mvn clean install
   ```

---

## Monitoring and Logging

### Health Check

```bash
curl http://localhost:8083/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "catalogService": {
          "status": "UP"
        }
      }
    },
    "db": {
      "status": "UP"
    }
  }
}
```

### Metrics

```bash
curl http://localhost:8083/actuator/metrics
```

### Log Files

Logs are written to:
- **Console:** Structured JSON format
- **File:** `logs/cart-service.log` (rotated daily, kept for 30 days)

**View logs:**
```bash
# Real-time logs
tail -f logs/cart-service.log

# Filter by correlation ID
cat logs/cart-service.log | grep "correlationId\":\"abc-123"

# Filter by error level
cat logs/cart-service.log | grep "\"level\":\"ERROR\""
```

---

## Architecture Notes

### Price Snapshot Strategy

The Cart Service stores a **snapshot** of the product price when an item is added to the cart:

- **Snapshot Time:** When item is first added to cart
- **Updates:** Price snapshot is NOT updated if product price changes in catalog
- **Rationale:** Provides price consistency for the user during their shopping session
- **Checkout:** Snapshot price is used for order total calculation

**Example:**
1. Product costs $100 when added to cart → snapshot = $100
2. Product price changes to $120 in catalog
3. Cart still shows $100 (snapshot)
4. Checkout uses $100 (snapshot)

### Duplicate Product Handling

When adding a product that already exists in the cart:

- **Behavior:** Increments the quantity of the existing cart item
- **Price:** Keeps the original price snapshot (does NOT update to current catalog price)
- **SKU/Title:** Keeps the original snapshots

**Example:**
1. Add Product A (qty: 2, price: $100)
2. Add Product A again (qty: 3)
3. Result: Single cart item for Product A (qty: 5, price: $100)

### Cart Lifecycle

**ACTIVE Cart:**
- One per user
- Mutable (items can be added/removed/updated)
- Default state for new carts

**CHECKED_OUT Cart:**
- Immutable snapshot
- Preserved for order creation
- New ACTIVE cart automatically created for user

---

## Future Enhancements

**Not included in MVP (can be added later):**

- [ ] JWT authentication and authorization
- [ ] Cart abandonment tracking and scheduled cleanup
- [ ] Stock availability validation before adding to cart
- [ ] Price synchronization warnings on checkout
- [ ] Multi-currency support
- [ ] Caching layer for product information (Redis)
- [ ] Event emission (CartCreated, CartCheckedOut, etc.)
- [ ] Integration with Order Service for complete checkout flow
- [ ] Promotion/discount codes
- [ ] Guest cart to user cart migration

---

## Support

For issues or questions:
1. Check logs: `logs/cart-service.log`
2. Verify dependencies are running (PostgreSQL, Catalog Service)
3. Check Swagger UI for API documentation: http://localhost:8083/swagger-ui.html
4. Review the implementation plan: `.claude/plans/elegant-conjuring-spindle.md`

---

## License

Part of the Digital Marketplace platform.
