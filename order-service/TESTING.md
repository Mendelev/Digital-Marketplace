# Order Service API Testing Guide

This guide provides manual testing steps for the Order Service. The Order Service orchestrates Cart and User services, performs mock payments, and reserves inventory.

## Prerequisites

1. **Start PostgreSQL for Order Service:**
   ```bash
   cd /Users/yuri.camargo/DevPro/Practice_projects/Digital-Marketplace/order-service
   docker-compose up -d
   ```

2. **Start Kafka (required for order events):**
   ```bash
   cd /Users/yuri.camargo/DevPro/Practice_projects/Digital-Marketplace
   docker-compose -f docker-compose.kafka.yml up -d
   ```

3. **Start dependent services:**
   - Cart Service (port 8083)
   - User Service (port 8081)

   Cart Service depends on Catalog Service for product validation.

4. **Start Order Service:**
   ```bash
   # Option 1: Run locally with Maven
   mvn spring-boot:run

   # Option 2: Run in Docker container
   docker-compose -f docker-compose-full.yml up -d
   ```

5. **Verify Order Service is running:**
   ```bash
   curl http://localhost:8086/api-docs
   ```

---

## Environment Variables (Optional)

```bash
export ORDER_BASE_URL="http://localhost:8086"
export USER_ID="11111111-1111-1111-1111-111111111111"
export CART_ID="<replace-with-cart-id>"
export SHIPPING_ADDRESS_ID="<replace-with-shipping-address-id>"
export BILLING_ADDRESS_ID="<replace-with-billing-address-id>"
```

---

## Data Setup Notes

- Order Service fetches cart data from Cart Service using:
  `/api/v1/carts/internal/{cartId}`
- Order Service fetches address data from User Service using:
  `/api/v1/addresses/{addressId}`

Ensure those endpoints are reachable from Order Service for successful order creation.

---

## Order Management Tests

### Test 1: Create Order

**Request:**
```bash
curl -X POST "$ORDER_BASE_URL/api/v1/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "'"$USER_ID"'",
    "cartId": "'"$CART_ID"'",
    "shippingAddressId": "'"$SHIPPING_ADDRESS_ID"'",
    "billingAddressId": "'"$BILLING_ADDRESS_ID"'"
  }'
```

**Expected Response:**
- Status: `201 Created`
- Body includes `orderId`, `status`, and `items`

Note: Payment authorization is mocked with a 90% success rate. If you get `402 Payment required`, retry.

---

### Test 2: Get Order by ID

Use the `orderId` from the create response.

**Request:**
```bash
curl "$ORDER_BASE_URL/api/v1/orders/<orderId>"
```

**Expected Response:**
- Status: `200 OK`
- Body: Full order details

---

### Test 3: List Orders for User

**Request:**
```bash
curl "$ORDER_BASE_URL/api/v1/orders/user/$USER_ID?page=0&size=20"
```

**Expected Response:**
- Status: `200 OK`
- Body: Paginated list of order summaries

---

### Test 4: Cancel Order

**Request:**
```bash
curl -X POST "$ORDER_BASE_URL/api/v1/orders/<orderId>/cancel?reason=Customer%20requested"
```

**Expected Response:**
- Status: `200 OK`
- Body: Order status updated to `CANCELLED`

---

### Test 5: Update Order Status

**Request:**
```bash
curl -X PATCH "$ORDER_BASE_URL/api/v1/orders/<orderId>/status?status=SHIPPED"
```

**Expected Response:**
- Status: `200 OK`
- Body: Order status updated to `SHIPPED`
