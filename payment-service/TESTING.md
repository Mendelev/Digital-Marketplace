# Payment Service API Testing Guide

This guide provides manual testing steps for the Payment Service. It supports service-to-service operations (shared secret) and user-facing queries (JWT).

## Prerequisites

1. **Start PostgreSQL for Payment Service:**
   ```bash
   cd /Users/yuri.camargo/DevPro/Practice_projects/Digital-Marketplace/payment-service
   docker-compose up -d
   ```

2. **Start Auth Service (required for JWT validation):**
   - Auth Service runs on port 8080.

3. **Start Payment Service:**
   ```bash
   # Option 1: Run locally with Maven
   mvn spring-boot:run

   # Option 2: Run in Docker container
   docker-compose -f docker-compose-full.yml up -d
   ```

4. **Verify Payment Service is running:**
   ```bash
   # Docker compose maps to host port 8087 by default
   curl http://localhost:8087/api-docs
   ```

---

## Environment Variables (Optional)

```bash
# If running locally (non-docker), use http://localhost:8083
export PAYMENT_BASE_URL="http://localhost:8087"
export PAYMENT_SERVICE_SECRET="dev-secret-change-in-production"
export ORDER_ID="11111111-1111-1111-1111-111111111111"
export USER_ID="22222222-2222-2222-2222-222222222222"
export PAYMENT_ID="5192aa58-11c1-4cd7-b357-665042891056"
```

---

## Authentication (JWT for user endpoints)

Get a JWT from the Auth Service:

```bash
export AUTH_TOKEN=$(curl -s -X POST "http://localhost:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin123!"}' | jq -r '.accessToken')
```

---

## Service-to-Service Tests (X-Service-Secret)

### Test 1: Create Payment

```bash
curl -X POST "$PAYMENT_BASE_URL/api/v1/payments" \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: $PAYMENT_SERVICE_SECRET" \
  -d '{
    "orderId": "'"$ORDER_ID"'",
    "userId": "'"$USER_ID"'",
    "amount": 49.99,
    "currency": "USD"
  }'
```

Save the `paymentId` from the response for the next steps.

---

### Test 2: Authorize Payment

```bash
curl -X POST "$PAYMENT_BASE_URL/api/v1/payments/$PAYMENT_ID/authorize" \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: $PAYMENT_SERVICE_SECRET" \
  -d '{
    "idempotencyKey": "auth-1"
  }'
```

Note: Authorization is simulated. If you receive `402 Payment Required`, retry.

---

### Test 3: Capture Payment (Full or Partial)

```bash
curl -X POST "$PAYMENT_BASE_URL/api/v1/payments/$PAYMENT_ID/capture" \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: $PAYMENT_SERVICE_SECRET" \
  -d '{
    "amount": 49.99,
    "idempotencyKey": "capture-1"
  }'
```

---

### Test 4: Refund Payment (Service Secret)

```bash
curl -X POST "$PAYMENT_BASE_URL/api/v1/payments/$PAYMENT_ID/refund" \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: $PAYMENT_SERVICE_SECRET" \
  -d '{
    "amount": 10.00,
    "reason": "Customer requested cancellation",
    "idempotencyKey": "refund-1"
  }'
```

---

### Test 5: Void Payment

```bash
curl -X POST "$PAYMENT_BASE_URL/api/v1/payments/$PAYMENT_ID/void" \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: $PAYMENT_SERVICE_SECRET" \
  -d '{
    "idempotencyKey": "void-1"
  }'
```

---

## User-Facing Tests (JWT)

### Test 6: Get Payment by ID

```bash
curl "$PAYMENT_BASE_URL/api/v1/payments/$PAYMENT_ID" \
  -H "Authorization: Bearer $AUTH_TOKEN"
```

---

### Test 7: Get Payment by Order ID

```bash
curl "$PAYMENT_BASE_URL/api/v1/payments/order/$ORDER_ID" \
  -H "Authorization: Bearer $AUTH_TOKEN"
```

---

### Test 8: List Payments for User

```bash
curl "$PAYMENT_BASE_URL/api/v1/payments/user/$USER_ID" \
  -H "Authorization: Bearer $AUTH_TOKEN"
```

---

### Test 9: Payment Transactions

```bash
curl "$PAYMENT_BASE_URL/api/v1/payments/$PAYMENT_ID/transactions" \
  -H "Authorization: Bearer $AUTH_TOKEN"
```

---

## Notes

- Payment success rates are configurable in `payment-service/src/main/resources/application.yml`.
- If you receive `402` during authorize/capture/refund/void, retry (simulation may fail).
