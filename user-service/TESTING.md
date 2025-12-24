# User Service API Testing Guide

This guide provides manual testing steps for the User Service. The service requires Auth Service for JWT validation and uses service-to-service auth for internal operations.

## Prerequisites

1. **Start PostgreSQL for User Service:**
   ```bash
   # If using shared Postgres
   docker-compose -f /Users/yuri.camargo/DevPro/Practice_projects/Digital-Marketplace/docker-compose.postgres.yml up -d

   # If using the service-local Postgres
   cd /Users/yuri.camargo/DevPro/Practice_projects/Digital-Marketplace/user-service
   docker-compose up -d
   ```

2. **Run database migrations:**
   ```bash
   cd /Users/yuri.camargo/DevPro/Practice_projects/Digital-Marketplace/user-service
   mvn flyway:migrate
   ```
   If you are using the shared Postgres on port 5432, run:
   ```bash
   mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/user_db -Dflyway.user=user_user -Dflyway.password=user_pass
   ```

3. **Start the Auth Service (required for JWT validation):**
   Auth Service runs on port 8080 and exposes `/api/v1/auth/public-key`.

4. **Start the User Service:**
   ```bash
   # Option 1: Run locally with Maven
   mvn spring-boot:run

   # Option 2: Run in Docker container
   docker-compose -f docker-compose-full.yml up -d
   ```

5. **Verify User Service is running:**
   ```bash
   curl http://localhost:8081/api-docs
   ```

---

## Environment Variables (Optional)

```bash
export USER_BASE_URL="http://localhost:8081"
export AUTH_BASE_URL="http://localhost:8080"
export AUTH_SERVICE_SECRET="dev-secret-change-in-production"
export USER_ID="<replace-with-user-id>"
export ADDRESS_ID="<replace-with-address-id>"
```

---

## Authentication (JWT)

Get a JWT from Auth Service:

```bash
export ACCESS_TOKEN=$(curl -s -X POST "$AUTH_BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin123!"}' | jq -r '.accessToken')
```

---

## Service-to-Service Tests (X-Service-Secret)

### Test 1: Create User (Auth Service only)

```bash
curl -X POST "$USER_BASE_URL/api/v1/users" \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: $AUTH_SERVICE_SECRET" \
  -d '{
    "userId": "11111111-1111-1111-1111-111111111111",
    "email": "customer@example.com",
    "name": "Customer One",
    "phone": "+15555550123",
    "roles": ["CUSTOMER"]
  }'
```

Save the `userId` from the response.

---

### Test 2: Get User (Internal)

```bash
curl "$USER_BASE_URL/api/v1/users/internal/$USER_ID" \
  -H "X-Service-Secret: $AUTH_SERVICE_SECRET"
```

---

### Test 3: Delete User (Internal)

```bash
curl -X DELETE "$USER_BASE_URL/api/v1/users/$USER_ID" \
  -H "X-Service-Secret: $AUTH_SERVICE_SECRET"
```

Expected: `204 No Content`

---

## User Profile Tests (JWT required)

### Test 4: Get Current User

```bash
curl "$USER_BASE_URL/api/v1/users/me" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

### Test 5: Get User by ID

```bash
curl "$USER_BASE_URL/api/v1/users/$USER_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

### Test 6: Update User

```bash
curl -X PUT "$USER_BASE_URL/api/v1/users/$USER_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "email": "customer@example.com",
    "name": "Customer One Updated",
    "phone": "+15555550999"
  }'
```

---

## Address Tests (JWT required)

### Test 7: Create Address

```bash
curl -X POST "$USER_BASE_URL/api/v1/addresses" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "label": "Home",
    "recipientName": "Customer One",
    "line1": "123 Main St",
    "line2": "Apt 4B",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001",
    "country": "US",
    "phone": "+15555550123"
  }'
```

Save the `addressId` for later tests.

---

### Test 8: List Addresses

```bash
curl "$USER_BASE_URL/api/v1/addresses" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

### Test 9: Get Address by ID

```bash
curl "$USER_BASE_URL/api/v1/addresses/$ADDRESS_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

### Test 10: Update Address

```bash
curl -X PUT "$USER_BASE_URL/api/v1/addresses/$ADDRESS_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "label": "Home",
    "recipientName": "Customer One",
    "line1": "456 Broadway",
    "line2": null,
    "city": "New York",
    "state": "NY",
    "postalCode": "10012",
    "country": "US",
    "phone": "+15555550123"
  }'
```

---

### Test 11: Set Default Shipping

```bash
curl -X PATCH "$USER_BASE_URL/api/v1/addresses/$ADDRESS_ID/default-shipping" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

### Test 12: Set Default Billing

```bash
curl -X PATCH "$USER_BASE_URL/api/v1/addresses/$ADDRESS_ID/default-billing" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

---

### Test 13: Delete Address

```bash
curl -X DELETE "$USER_BASE_URL/api/v1/addresses/$ADDRESS_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Expected: `204 No Content`

---

## Internal Address Lookup (Service-to-Service)

```bash
curl "$USER_BASE_URL/api/v1/addresses/internal/$ADDRESS_ID" \
  -H "X-Service-Secret: $AUTH_SERVICE_SECRET"
```

---

## Notes

- JWT is required for `/api/v1/users/me` and address endpoints.
- Service-to-service endpoints require `X-Service-Secret`.
- If you get `401`, confirm Auth Service is running and the public key endpoint is reachable.
