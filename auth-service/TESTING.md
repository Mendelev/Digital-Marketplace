# Auth Service API Testing Guide

This guide provides manual testing procedures for the Auth Service API endpoints. Since the User Service is not yet available, the registration endpoint cannot be tested, but all other endpoints can be verified.

## Prerequisites

1. **Start Docker containers:**
   ```bash
   docker-compose up -d
   ```

2. **Run database migrations:**
   ```bash
   mvn flyway:migrate
   ```

3. **Start the Auth Service:**
   ```bash
   mvn spring-boot:run
   ```

4. **Access Swagger UI (Optional):**
   Open http://localhost:8080/swagger-ui.html for interactive API testing.

---

## Test 1: Get Public Key

Retrieve the RSA public key used for JWT verification.

**Request:**
```bash
curl http://localhost:8080/api/v1/auth/public-key
```

**Expected Response:**
- Status: `200 OK`
- Content-Type: `text/plain`
- Body: RSA public key in PEM format

---

## Test 2: Setup Test User

Since registration requires the User Service, manually insert a test user into the database.

**Connect to PostgreSQL:**
```bash
docker exec -it auth-service-postgres psql -U auth_user -d auth_db
```

**Insert test credential:**
```sql
-- Password is BCrypt hash of "password123"
INSERT INTO credentials (id, user_id, email, password_hash, status, failed_login_count, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890'::uuid,
    'test@example.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYfQzz3U5YK',
    'ACTIVE',
    0,
    NOW(),
    NOW()
);
```

**Verify insertion:**
```sql
SELECT id, user_id, email, status, failed_login_count FROM credentials WHERE email = 'test@example.com';
```

**Exit PostgreSQL:**
```sql
\q
```

---

## Test 3: Login (Valid Credentials)

Authenticate with valid credentials to receive access and refresh tokens.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

**Expected Response:**
- Status: `200 OK`
- Body:
  ```json
  {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "expiresIn": 900,
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "email": "test@example.com",
    "roles": ["CUSTOMER"]
  }
  ```

**Save the tokens** for subsequent tests:
```bash
# Example - extract and save tokens
export ACCESS_TOKEN="<paste_access_token_here>"
export REFRESH_TOKEN="<paste_refresh_token_here>"
```

---

## Test 4: Login (Invalid Password)

Test authentication failure with incorrect password.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "wrongpassword"
  }'
```

**Expected Response:**
- Status: `401 Unauthorized`
- Body:
  ```json
  {
    "timestamp": "2025-12-16T...",
    "status": 401,
    "error": "Unauthorized",
    "message": "Invalid email or password",
    "path": "/api/v1/auth/login",
    "correlationId": "..."
  }
  ```

---

## Test 5: Login (Non-existent Email)

Test authentication with email that doesn't exist.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "nonexistent@example.com",
    "password": "password123"
  }'
```

**Expected Response:**
- Status: `401 Unauthorized`
- Error message: "Invalid email or password"

---

## Test 6: Account Lockout

Test account lockout after 5 failed login attempts.

**Request (run 6 times):**
```bash
for i in {1..6}; do
  echo "=== Attempt $i ==="
  curl -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{
      "email": "test@example.com",
      "password": "wrongpassword"
    }'
  echo ""
done
```

**Expected Results:**
- Attempts 1-4: `401 Unauthorized` with "Invalid email or password"
- Attempts 5-6: `423 Locked` with "Account has been locked due to too many failed login attempts"

**Verify in database:**
```bash
docker exec -it auth-service-postgres psql -U auth_user -d auth_db -c "SELECT email, status, failed_login_count FROM credentials WHERE email = 'test@example.com';"
```

**Reset the account for further testing:**
```bash
docker exec -it auth-service-postgres psql -U auth_user -d auth_db -c "UPDATE credentials SET status = 'ACTIVE', failed_login_count = 0 WHERE email = 'test@example.com';"
```

---

## Test 7: Token Validation (Valid Token)

Validate a JWT access token.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/validate \
  -H "Content-Type: application/json" \
  -d "{
    \"token\": \"$ACCESS_TOKEN\"
  }"
```

**Expected Response:**
- Status: `200 OK`
- Body:
  ```json
  {
    "valid": true,
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "email": "test@example.com",
    "roles": ["CUSTOMER"],
    "expiresAt": "2025-12-16T..."
  }
  ```

---

## Test 8: Token Validation (Invalid Token)

Test validation with a malformed or invalid token.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/validate \
  -H "Content-Type: application/json" \
  -d '{
    "token": "invalid.jwt.token"
  }'
```

**Expected Response:**
- Status: `401 Unauthorized`
- Error message: "Invalid token"

---

## Test 9: Token Validation (Expired Token)

Wait for the access token to expire (15 minutes) or use an old token.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/validate \
  -H "Content-Type: application/json" \
  -d '{
    "token": "<expired_token>"
  }'
```

**Expected Response:**
- Status: `401 Unauthorized`
- Error message: "Token has expired"

---

## Test 10: Refresh Token (Valid)

Get a new access token using a valid refresh token.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{
    \"refreshToken\": \"$REFRESH_TOKEN\"
  }"
```

**Expected Response:**
- Status: `200 OK`
- Body: New `accessToken` and `refreshToken` (old refresh token is revoked)

**Save new tokens:**
```bash
export ACCESS_TOKEN="<new_access_token>"
export REFRESH_TOKEN="<new_refresh_token>"
```

---

## Test 11: Refresh Token (Reuse Revoked Token)

Attempt to reuse the old refresh token (should fail).

**Request:**
```bash
# Use the OLD refresh token from Test 3
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<old_revoked_token>"
  }'
```

**Expected Response:**
- Status: `401 Unauthorized`
- Error message: "Refresh token has been revoked" or "Invalid refresh token"

---

## Test 12: Forgot Password

Request a password reset token.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com"
  }'
```

**Expected Response:**
- Status: `200 OK`
- Body:
  ```json
  {
    "message": "If the email address exists, a password reset link has been sent."
  }
  ```

**Check application logs** for the reset token (it would normally be sent via email):
```bash
# Look for log entry with the reset token
```

**Or query the database:**
```bash
docker exec -it auth-service-postgres psql -U auth_user -d auth_db -c "SELECT token_hash, email, expires_at, used_at FROM password_reset_tokens WHERE email = 'test@example.com' ORDER BY created_at DESC LIMIT 1;"
```

**Note:** The token in the database is hashed. You need to extract the actual token from the application logs (look for "Reset token (would be sent via email): ...")

---

## Test 13: Forgot Password (Non-existent Email)

Request password reset for non-existent email (should return success for security).

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "nonexistent@example.com"
  }'
```

**Expected Response:**
- Status: `200 OK`
- Same message as Test 12 (security: don't reveal if email exists)

---

## Test 14: Reset Password (Valid Token)

Reset password using a valid reset token.

**First, get the reset token from logs:**
```bash
# Look in application logs for: "Reset token (would be sent via email): <TOKEN>"
export RESET_TOKEN="<token_from_logs>"
```

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d "{
    \"token\": \"$RESET_TOKEN\",
    \"newPassword\": \"newpassword456\"
  }"
```

**Expected Response:**
- Status: `200 OK`
- Body:
  ```json
  {
    "message": "Password has been reset successfully"
  }
  ```

**Verify by logging in with new password:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "newpassword456"
  }'
```

---

## Test 15: Reset Password (Invalid/Expired Token)

Attempt password reset with invalid token.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "invalid-reset-token",
    "newPassword": "newpassword789"
  }'
```

**Expected Response:**
- Status: `401 Unauthorized`
- Error message: "Invalid or expired reset token"

---

## Test 16: Reset Password (Reuse Token)

Attempt to reuse a password reset token (should fail).

**Request:**
```bash
# Use the same token from Test 14
curl -X POST http://localhost:8080/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d "{
    \"token\": \"$RESET_TOKEN\",
    \"newPassword\": \"anotherpassword\"
  }"
```

**Expected Response:**
- Status: `401 Unauthorized`
- Error message: "Reset token has already been used"

---

## Test 17: Token Cleanup

Delete old refresh tokens from the database.

**First, create some old tokens in the database:**
```bash
docker exec -it auth-service-postgres psql -U auth_user -d auth_db -c "INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at, created_at) VALUES (gen_random_uuid(), 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'::uuid, 'old_token_hash_1', NOW() - INTERVAL '35 days', NOW() - INTERVAL '35 days');"
```

**Request:**
```bash
curl -X DELETE http://localhost:8080/api/v1/auth/tokens/cleanup \
  -H "Content-Type: application/json" \
  -d '{
    "olderThanDays": 30,
    "includeActive": false
  }'
```

**Expected Response:**
- Status: `200 OK`
- Body:
  ```json
  {
    "deletedCount": 1,
    "message": "Successfully deleted 1 refresh tokens"
  }
  ```

---

## Test 18: Validation Errors

Test input validation with invalid data.

**Invalid email format:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "not-an-email",
    "password": "password123"
  }'
```

**Expected Response:**
- Status: `400 Bad Request`
- Error message mentioning email validation failure

**Password too short:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "123"
  }'
```

**Expected Response:**
- Status: `400 Bad Request`
- Error message mentioning password length requirement

**Missing required fields:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com"
  }'
```

**Expected Response:**
- Status: `400 Bad Request`
- Error message mentioning missing password field

---

## Test 19: Registration (Expected to Fail)

Attempt registration without User Service (demonstrates circuit breaker).

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "password": "password123",
    "name": "New User"
  }'
```

**Expected Response:**
- Status: `503 Service Unavailable`
- Body:
  ```json
  {
    "timestamp": "2025-12-16T...",
    "status": 503,
    "error": "Service Unavailable",
    "message": "User Service is temporarily unavailable. Please try again later.",
    "path": "/api/v1/auth/register",
    "correlationId": "..."
  }
  ```

---

## Test 20: Correlation ID Tracking

Verify that correlation IDs are propagated through requests.

**Request with custom correlation ID:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: test-correlation-123" \
  -d '{
    "email": "test@example.com",
    "password": "wrongpassword"
  }'
```

**Expected:**
- Response should include the same correlation ID in error response
- Application logs should show `correlationId=test-correlation-123`

**Request without correlation ID (auto-generated):**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "wrongpassword"
  }'
```

**Expected:**
- Response includes a generated UUID as correlation ID
- Check response header: `X-Correlation-ID`

---

## Cleanup

After testing, you can clean up test data:

```bash
# Connect to database
docker exec -it auth-service-postgres psql -U auth_user -d auth_db

-- Delete test data
DELETE FROM password_reset_tokens WHERE email = 'test@example.com';
DELETE FROM refresh_tokens WHERE user_id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890'::uuid;
DELETE FROM credentials WHERE email = 'test@example.com';

\q
```

**Stop services:**
```bash
# Stop Auth Service (Ctrl+C if running in terminal)

# Stop Docker containers
docker-compose down
```

---

## Notes

- **Access tokens** expire in 15 minutes
- **Refresh tokens** expire in 7 days
- **Password reset tokens** expire in 1 hour
- **Account lockout** occurs after 5 failed login attempts
- All timestamps are in ISO-8601 format
- All errors include correlation IDs for tracing

## Next Steps

Once the User Service is available:
1. Start Mockoon mock: `docker-compose up -d mockoon`
2. Test the registration endpoint (Test 19 should succeed)
3. Verify User Service integration with circuit breaker behavior
