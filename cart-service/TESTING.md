# Cart Service API Testing Guide

This guide covers manual testing steps for the Cart Service API. The Cart Service manages shopping carts and depends on the Catalog Service for product validation.

## Prerequisites

1. **Start PostgreSQL for Cart Service:**
   ```bash
   cd /Users/yuri.camargo/DevPro/Practice_projects/Digital-Marketplace/cart-service
   docker-compose up -d
   ```

2. **Run database migrations:**
   ```bash
   mvn flyway:migrate
   ```

3. **Start Catalog Service (required for product validation):**
   - Follow `catalog-service/TESTING.md` to start Catalog Service and create at least one ACTIVE product.
   - Make note of a valid `productId`.

4. **Start the Cart Service:**
   ```bash
   # Option 1: Run locally with Maven
   mvn spring-boot:run

   # Option 2: Run in Docker container
   docker-compose -f docker-compose-full.yml up -d
   ```

5. **Verify service is running:**
   ```bash
   curl http://localhost:8083/actuator/health
   ```

---

## Environment Variables (Optional)

```bash
export CART_BASE_URL="http://localhost:8083"
export USER_ID="11111111-1111-1111-1111-111111111111"
export PRODUCT_ID="20787d17-0db8-4bfc-9c3d-4f8068646ecd"
```

---

## Cart Management Tests

### Test 1: Get or Create Cart

**Request:**
```bash
curl "$CART_BASE_URL/api/v1/carts/$USER_ID"
```

**Expected Response:**
- Status: `200 OK`
- Body: Cart details with status `ACTIVE`

---

### Test 2: Add Item to Cart

**Request:**
```bash
curl -X POST "$CART_BASE_URL/api/v1/carts/$USER_ID/items" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "'"$PRODUCT_ID"'",
    "quantity": 2
  }'
```

**Expected Response:**
- Status: `200 OK`
- Body: Cart with `items` containing the added product

---

### Test 3: Update Item Quantity

Use the `cartItemId` from the previous response.

**Request:**
```bash
curl -X PUT "$CART_BASE_URL/api/v1/carts/$USER_ID/items/4c2dd5a8-0c75-434c-9de4-5f82d1b23815" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 3
  }'

```

**Expected Response:**
- Status: `200 OK`
- Body: Cart with updated item quantity

---

### Test 4: Remove Item from Cart

**Request:**
```bash
curl -X DELETE "$CART_BASE_URL/api/v1/carts/$USER_ID/items/4c2dd5a8-0c75-434c-9de4-5f82d1b23815"
```

**Expected Response:**
- Status: `204 No Content`

---

### Test 5: Clear Cart

**Request:**
```bash
curl -X DELETE "$CART_BASE_URL/api/v1/carts/$USER_ID"
```

**Expected Response:**
- Status: `204 No Content`

---

### Test 6: Checkout Cart

Ensure the cart has at least one item before checkout.

**Request:**
```bash
curl -X POST "$CART_BASE_URL/api/v1/carts/$USER_ID/checkout"
```

**Expected Response:**
- Status: `200 OK`
- Body: Checkout response with cart snapshot and success message

---

## Error Cases (Optional)

### Add Item with Invalid Quantity

**Request:**
```bash
curl -X POST "$CART_BASE_URL/api/v1/carts/$USER_ID/items" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "'"$PRODUCT_ID"'",
    "quantity": 0
  }'
```

**Expected Response:**
- Status: `400 Bad Request`

### Add Item When Catalog Service Is Down

Stop Catalog Service and attempt to add an item.

**Expected Response:**
- Status: `503 Service Unavailable`
