# Catalog Service API Testing Guide

This guide provides manual testing procedures for the Catalog Service API endpoints. The Catalog Service manages products and categories with multi-seller support, role-based authorization, and event publishing.

## Prerequisites

1. **Start dependencies (PostgreSQL + Kafka):**
   ```bash
   cd /Users/yuri.camargo/DevPro/Practice_projects/Digital-Marketplace/catalog-service
   docker-compose up -d
   ```
   
   This will start:
   - PostgreSQL on port **5464** with database `catalog_db`
   - Zookeeper for Kafka coordination
   - Kafka on port **9092** with auto-topic creation

2. **Run database migrations:**
   ```bash
   mvn flyway:migrate
   ```

3. **Set JWT secret (should match Auth Service):**
   ```bash
   export JWT_SECRET="your-secret-key-at-least-256-bits-long-for-HS256-algorithm"
   ```

4. **Start the Catalog Service:**
   ```bash
   # Option 1: Run locally with Maven
   mvn spring-boot:run
   
   # Option 2: Run in Docker container
   docker-compose -f docker-compose-full.yml up -d
   ```

7. **Verify service is running:**
   ```bash
   curl http://localhost:8082/actuator/health
   ```

---

## Generate Test JWT Tokens

Since the Catalog Service validates JWTs from Auth Service, you need valid tokens. Use the Auth Service to generate tokens or create test tokens manually.

### Option 1: Using Auth Service (Recommended)

```bash
# Login via Auth Service to get tokens
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "Admin123!"
  }'

# Save the access token
export ADMIN_TOKEN="<access_token_with_ADMIN_role>"
export SELLER_TOKEN="<access_token_with_SELLER_role>"
export CUSTOMER_TOKEN="<access_token_with_CUSTOMER_role>"
```

### Option 2: Create Test Tokens Manually (For Testing Only)

You can use https://jwt.io to create test tokens with the following payloads:

**Admin Token Payload:**
```json
{
  "sub": "admin@example.com",
  "userId": "11111111-1111-1111-1111-111111111111",
  "email": "admin@example.com",
  "roles": ["ADMIN"],
  "iat": 1734451200,
  "exp": 9999999999
}
```

**Seller Token Payload:**
```json
{
  "sub": "seller@example.com",
  "userId": "22222222-2222-2222-2222-222222222222",
  "email": "seller@example.com",
  "roles": ["SELLER"],
  "iat": 1734451200,
  "exp": 9999999999
}
```

**Customer Token Payload:**
```json
{
  "sub": "customer@example.com",
  "userId": "33333333-3333-3333-3333-333333333333",
  "email": "customer@example.com",
  "roles": ["CUSTOMER"],
  "iat": 1734451200,
  "exp": 9999999999
}
```

Sign these with your JWT_SECRET using HS256 algorithm.

---

## Category Management Tests

### Test 1: Create Category (Admin Only)

Create a top-level category.

**Request:**
```bash
curl -X POST http://localhost:8082/api/v1/categories \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "Electronics",
    "description": "Electronic devices and accessories"
  }'
```

**Expected Response:**
- Status: `201 Created`
- Body:
  ```json
  {
    "id": 1,
    "name": "Electronics",
    "description": "Electronic devices and accessories",
    "parentCategoryId": null,
    "parentCategoryName": null,
    "createdAt": "2025-12-17T...",
    "updatedAt": "2025-12-17T..."
  }
  ```

---

### Test 2: Create Subcategory

Create a category with a parent.

**Request:**
```bash
curl -X POST http://localhost:8082/api/v1/categories \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "Smartphones",
    "description": "Mobile phones and smartphones",
    "parentCategoryId": 1
  }'
```

**Expected Response:**
- Status: `201 Created`
- Body includes `parentCategoryId: 1` and `parentCategoryName: "Electronics"`

---

### Test 3: Create Category (Unauthorized - No Token)

Attempt to create category without authentication.

**Request:**
```bash
curl -X POST http://localhost:8082/api/v1/categories \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Unauthorized Category",
    "description": "This should fail"
  }'
```

**Expected Response:**
- Status: `401 Unauthorized` or `403 Forbidden`

---

### Test 4: Create Category (Forbidden - Seller Token)

Attempt to create category with non-admin token.

**Request:**
```bash
curl -X POST http://localhost:8082/api/v1/categories \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $SELLER_TOKEN" \
  -d '{
    "name": "Forbidden Category",
    "description": "This should fail"
  }'
```

**Expected Response:**
- Status: `403 Forbidden`

---

### Test 5: Get Category by ID (Public)

Retrieve a specific category (no authentication required).

**Request:**
```bash
curl http://localhost:8082/api/v1/categories/1
```

**Expected Response:**
- Status: `200 OK`
- Body: Category details with id=1

---

### Test 6: Get Category (Not Found)

Request non-existent category.

**Request:**
```bash
curl http://localhost:8082/api/v1/categories/999
```

**Expected Response:**
- Status: `404 Not Found`
- Error message: "Category not found with id: 999"

---

### Test 7: Get All Categories (Public)

List all categories.

**Request:**
```bash
curl http://localhost:8082/api/v1/categories
```

**Expected Response:**
- Status: `200 OK`
- Body: Array of all categories

---

### Test 8: Get Top-Level Categories

List categories without parent.

**Request:**
```bash
curl http://localhost:8082/api/v1/categories/top-level
```

**Expected Response:**
- Status: `200 OK`
- Body: Array of categories where `parentCategoryId` is null

---

### Test 9: Get Subcategories

List subcategories of a specific category.

**Request:**
```bash
curl http://localhost:8082/api/v1/categories/1/subcategories
```

**Expected Response:**
- Status: `200 OK`
- Body: Array of categories where `parentCategoryId` equals 1

---

### Test 10: Delete Category (Admin Only)

Delete a category.

**Request:**
```bash
curl -X DELETE http://localhost:8082/api/v1/categories/2 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Expected Response:**
- Status: `204 No Content`

---

### Test 11: Duplicate Category Name

Attempt to create category with existing name.

**Request:**
```bash
curl -X POST http://localhost:8082/api/v1/categories \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "Electronics",
    "description": "Duplicate category"
  }'
```

**Expected Response:**
- Status: `409 Conflict`
- Error message: "Category with name 'Electronics' already exists"

---

## Product Management Tests

### Test 12: Create Product (Seller)

Create a product as a seller.

**Request:**
```bash
curl -X POST http://localhost:8082/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $SELLER_TOKEN" \
  -d '{
    "name": "iPhone 15 Pro",
    "description": "Latest Apple smartphone with A17 Pro chip",
    "basePrice": 999.99,
    "categoryId": 2,
    "availableSizes": ["128GB", "256GB", "512GB"],
    "availableColors": ["Black", "White", "Blue"],
    "stockPerVariant": {
      "128GB-Black": 10,
      "128GB-White": 5,
      "256GB-Black": 8,
      "256GB-Blue": 3
    },
    "imageUrls": ["https://via.placeholder.com/400x400"],
    "status": "ACTIVE"
  }'
```

**Expected Response:**
- Status: `201 Created`
- Body: Product details with generated UUID `id`
- `sellerId` should match the seller's userId from token

**Note:** Image URLs will be validated with HTTP HEAD requests. Use valid URLs or disable validation for testing.

---

### Test 13: Create Product (Admin for Another Seller)

Admin creates product on behalf of another seller.

**Request:**
```bash
curl -X POST http://localhost:8082/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "Samsung Galaxy S24",
    "description": "Latest Samsung flagship",
    "basePrice": 899.99,
    "categoryId": 2,
    "sellerId": "22222222-2222-2222-2222-222222222222",
    "availableSizes": ["128GB", "256GB"],
    "availableColors": ["Black", "White"],
    "stockPerVariant": {
      "128GB-Black": 15,
      "256GB-White": 10
    },
    "imageUrls": ["https://via.placeholder.com/400x400"],
    "status": "ACTIVE"
  }'
```

**Expected Response:**
- Status: `201 Created`
- `sellerId` should be the specified UUID

---

### Test 14: Create Product (Invalid Image URL)

Attempt to create product with invalid image URL.

**Request:**
```bash
curl -X POST http://localhost:8082/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $SELLER_TOKEN" \
  -d '{
    "name": "Test Product",
    "description": "Product with invalid image",
    "basePrice": 99.99,
    "categoryId": 1,
    "availableSizes": ["S"],
    "availableColors": ["Red"],
    "stockPerVariant": {"S-Red": 10},
    "imageUrls": ["https://invalid-domain-that-does-not-exist.com/image.jpg"],
    "status": "ACTIVE"
  }'
```

**Expected Response:**
- Status: `400 Bad Request`
- Error message: "Invalid or inaccessible URLs: ..."

---

### Test 15: Get Product by ID (Public)

Retrieve a specific product.

**Request:**
```bash
curl http://localhost:8082/api/v1/products/{productId}
```

**Expected Response:**
- Status: `200 OK`
- Body: Complete product details

---

### Test 16: List All Products (Public, Paginated)

List products with pagination and filters.

**Request:**
```bash
# Basic pagination
curl "http://localhost:8082/api/v1/products?page=0&size=10&sort=createdAt,desc"

# Filter by category
curl "http://localhost:8082/api/v1/products?categoryId=2&page=0&size=10"

# Filter by seller
curl "http://localhost:8082/api/v1/products?sellerId=22222222-2222-2222-2222-222222222222&page=0&size=10"

# Filter by status
curl "http://localhost:8082/api/v1/products?status=ACTIVE&page=0&size=10"

# Combined filters
curl "http://localhost:8082/api/v1/products?categoryId=2&status=ACTIVE&page=0&size=10"
```

**Expected Response:**
- Status: `200 OK`
- Body: Paginated response with products, page info
  ```json
  {
    "content": [...],
    "pageable": {...},
    "totalPages": 1,
    "totalElements": 2,
    "size": 10,
    "number": 0
  }
  ```

---

### Test 17: Get Seller's Products

Seller retrieves their own products.

**Request:**
```bash
curl http://localhost:8082/api/v1/products/seller/my-products?page=0&size=10 \
  -H "Authorization: Bearer $SELLER_TOKEN"
```

**Expected Response:**
- Status: `200 OK`
- Body: Paginated list of products belonging to the seller

---

### Test 18: Get Featured Products (Public)

Retrieve featured products.

**Request:**
```bash
curl http://localhost:8082/api/v1/products/featured?page=0&size=10
```

**Expected Response:**
- Status: `200 OK`
- Body: Paginated list of products where `featured=true`

---

### Test 19: Update Product (Owner)

Seller updates their own product.

**Request:**
```bash
curl -X PUT http://localhost:8082/api/v1/products/{productId} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $SELLER_TOKEN" \
  -d '{
    "name": "iPhone 15 Pro (Updated)",
    "basePrice": 949.99,
    "status": "ACTIVE"
  }'
```

**Expected Response:**
- Status: `200 OK`
- Body: Updated product details
- If price changed, a record should be created in `product_price_history` table

---

### Test 20: Update Product (Wrong Seller)

Seller attempts to update another seller's product.

**Request:**
```bash
# First, get a product ID belonging to a different seller
# Then try to update it with SELLER_TOKEN
curl -X PUT http://localhost:8082/api/v1/products/{other_seller_productId} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $SELLER_TOKEN" \
  -d '{
    "basePrice": 1.00
  }'
```

**Expected Response:**
- Status: `403 Forbidden`
- Error message: "You do not have permission to access this product"

---

### Test 21: Update Product (Admin)

Admin updates any product.

**Request:**
```bash
curl -X PUT http://localhost:8082/api/v1/products/{any_productId} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "featured": true,
    "status": "ACTIVE"
  }'
```

**Expected Response:**
- Status: `200 OK`
- Body: Updated product (admin can update any product)

---

### Test 22: Update Product Price (Track History)

Update product price and verify history tracking.

**Request:**
```bash
# First update
curl -X PUT http://localhost:8082/api/v1/products/{productId} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $SELLER_TOKEN" \
  -d '{
    "basePrice": 899.99
  }'

# Second update
curl -X PUT http://localhost:8082/api/v1/products/{productId} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $SELLER_TOKEN" \
  -d '{
    "basePrice": 849.99
  }'
```

**Verify in database:**
```bash
docker exec -it catalog-service-postgres psql -U catalog_user -d catalog_db -c "SELECT product_id, old_price, new_price, changed_at, changed_by FROM product_price_history ORDER BY changed_at DESC LIMIT 5;"
```

---

### Test 23: Delete Product (Owner)

Seller deletes their own product.

**Request:**
```bash
curl -X DELETE http://localhost:8082/api/v1/products/{productId} \
  -H "Authorization: Bearer $SELLER_TOKEN"
```

**Expected Response:**
- Status: `204 No Content`

**Verify product is deleted:**
```bash
curl http://localhost:8082/api/v1/products/{productId}
# Should return 404
```

---

### Test 24: Delete Product (Wrong Seller)

Seller attempts to delete another seller's product.

**Request:**
```bash
curl -X DELETE http://localhost:8082/api/v1/products/{other_seller_productId} \
  -H "Authorization: Bearer $SELLER_TOKEN"
```

**Expected Response:**
- Status: `403 Forbidden`

---

### Test 25: Delete Product (Admin)

Admin deletes any product.

**Request:**
```bash
curl -X DELETE http://localhost:8082/api/v1/products/{any_productId} \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Expected Response:**
- Status: `204 No Content`

---

## Event Publishing Tests

### Test 26: Verify Product Created Event

Check that product creation publishes an event to Kafka.

**Steps:**
1. Start a Kafka consumer to listen to the `product-events` topic:
   ```bash
   # Using kafka-console-consumer
   kafka-console-consumer --bootstrap-server localhost:9092 \
     --topic product-events \
     --from-beginning \
     --property print.key=true
   ```

2. Create a product (Test 12)

3. Check consumer output for event like:
   ```json
   {
     "eventId": "...",
     "productId": "...",
     "eventType": "PRODUCT_CREATED",
     "sequenceNumber": 1,
     "payload": {
       "productId": "...",
       "sellerId": "...",
       "name": "iPhone 15 Pro",
       ...
     },
     "publishedAt": "2025-12-17T..."
   }
   ```

**Verify in database:**
```bash
docker exec -it catalog-service-postgres psql -U catalog_user -d catalog_db -c "SELECT event_id, product_id, event_type, sequence_number, published_at FROM product_events ORDER BY sequence_number DESC LIMIT 5;"
```

---

### Test 27: Verify Event Ordering

Create multiple products and verify sequence numbers are incremental.

**Request:**
```bash
# Create 3 products quickly
for i in {1..3}; do
  curl -X POST http://localhost:8082/api/v1/products \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $SELLER_TOKEN" \
    -d "{
      \"name\": \"Test Product $i\",
      \"description\": \"Test product number $i\",
      \"basePrice\": 99.99,
      \"categoryId\": 1,
      \"availableSizes\": [\"S\"],
      \"availableColors\": [\"Red\"],
      \"stockPerVariant\": {\"S-Red\": 10},
      \"imageUrls\": [\"https://via.placeholder.com/400x400\"],
      \"status\": \"ACTIVE\"
    }"
done
```

**Verify sequence numbers:**
```bash
docker exec -it catalog-service-postgres psql -U catalog_user -d catalog_db -c "SELECT event_type, sequence_number, product_id FROM product_events ORDER BY sequence_number;"
```

Sequence numbers should be consecutive (1, 2, 3, ...).

---

### Test 28: Verify Event Idempotency

Check that duplicate event IDs are prevented.

**Verify in database:**
```bash
# event_id column has UNIQUE constraint
docker exec -it catalog-service-postgres psql -U catalog_user -d catalog_db -c "\d product_events"
```

---

## Search Service Integration Tests

### Test 29: Verify Search Service Indexing

**Prerequisites:** Start a mock search service or check logs for search service calls.

**Request:** Create a product (Test 12)

**Check application logs:**
```bash
# Look for log entries like:
# "Indexed product {productId} in search service"
# or
# "Circuit breaker activated: Failed to index product {productId}"
```

---

### Test 30: Search Service Circuit Breaker

Stop the search service and verify the circuit breaker prevents cascading failures.

**Steps:**
1. Stop search service (if running)
2. Create multiple products
3. Verify:
   - Products are still created successfully
   - Circuit breaker logs appear after threshold failures
   - Service remains responsive

---

## Validation Tests

### Test 31: Invalid Product Data

Test various validation failures.

**Missing required fields:**
```bash
curl -X POST http://localhost:8082/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $SELLER_TOKEN" \
  -d '{
    "name": "Test Product"
  }'
```

**Expected:** `400 Bad Request` with validation errors

**Negative price:**
```bash
curl -X POST http://localhost:8082/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $SELLER_TOKEN" \
  -d '{
    "name": "Test Product",
    "description": "Test",
    "basePrice": -10.00,
    "categoryId": 1,
    "imageUrls": ["https://via.placeholder.com/400x400"]
  }'
```

**Expected:** `400 Bad Request` with price validation error

**Empty image URLs:**
```bash
curl -X POST http://localhost:8082/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $SELLER_TOKEN" \
  -d '{
    "name": "Test Product",
    "description": "Test",
    "basePrice": 99.99,
    "categoryId": 1,
    "imageUrls": []
  }'
```

**Expected:** `400 Bad Request` with image URL validation error

---

### Test 32: Invalid Category Data

**Blank category name:**
```bash
curl -X POST http://localhost:8082/api/v1/categories \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "",
    "description": "Test"
  }'
```

**Expected:** `400 Bad Request`

**Non-existent parent category:**
```bash
curl -X POST http://localhost:8082/api/v1/categories \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "Test Category",
    "parentCategoryId": 999
  }'
```

**Expected:** `404 Not Found` with "Parent category not found"

---

## Database Verification Tests

### Test 33: Verify Product Variants Storage

Check that product variants are stored correctly as JSONB.

**Query:**
```bash
docker exec -it catalog-service-postgres psql -U catalog_user -d catalog_db -c "SELECT id, name, available_sizes, available_colors, stock_per_variant FROM products LIMIT 1;"
```

**Expected:** Arrays and JSON properly stored

---

### Test 34: Verify Category Hierarchy

Check parent-child relationships.

**Query:**
```bash
docker exec -it catalog-service-postgres psql -U catalog_user -d catalog_db -c "SELECT c1.name as category, c2.name as parent FROM categories c1 LEFT JOIN categories c2 ON c1.parent_category_id = c2.id;"
```

---

### Test 35: Verify Price History

**Query:**
```bash
docker exec -it catalog-service-postgres psql -U catalog_user -d catalog_db -c "SELECT p.name, ph.old_price, ph.new_price, ph.changed_at FROM product_price_history ph JOIN products p ON ph.product_id = p.id ORDER BY ph.changed_at DESC;"
```

---

## Performance Tests

### Test 36: Pagination Performance

Test large result sets.

**Create many products:**
```bash
for i in {1..50}; do
  curl -X POST http://localhost:8082/api/v1/products \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $SELLER_TOKEN" \
    -d "{
      \"name\": \"Product $i\",
      \"description\": \"Test product $i\",
      \"basePrice\": 99.99,
      \"categoryId\": 1,
      \"availableSizes\": [\"S\"],
      \"availableColors\": [\"Red\"],
      \"stockPerVariant\": {\"S-Red\": 10},
      \"imageUrls\": [\"https://via.placeholder.com/400x400\"]
    }"
done
```

**Test pagination:**
```bash
# First page
time curl "http://localhost:8082/api/v1/products?page=0&size=10"

# Last page
time curl "http://localhost:8082/api/v1/products?page=4&size=10"
```

---

## Cleanup

After testing:

```bash
# Connect to database
docker exec -it catalog-service-postgres psql -U catalog_user -d catalog_db

-- View test data
SELECT id, name FROM categories;
SELECT id, name, seller_id FROM products;

-- Delete test data (cascades to related tables)
DELETE FROM products;
DELETE FROM categories;

-- Reset sequences
ALTER SEQUENCE categories_id_seq RESTART WITH 1;

\q
```

**Stop services:**
```bash
# Stop Catalog Service (Ctrl+C)

# Stop Docker containers
docker-compose down
```

---

## Notes

- **Access tokens** should be obtained from Auth Service
- **Admin role** required for category create/delete
- **Seller role** required for product create/update/delete (own products only)
- **Public access** for GET operations on products and categories
- **Image validation** performs HTTP HEAD requests (use valid URLs)
- **Price changes** are automatically tracked in `product_price_history`
- **Events** are published to Kafka topic `product-events` with sequence numbers
- **Circuit breaker** protects against Search Service failures
- All **timestamps** are in ISO-8601 format with UTC timezone

---

## Integration with Other Services

### Auth Service Integration
- JWT tokens validated on secured endpoints
- User ID extracted from token for seller identification
- Roles checked for authorization

### Search Service Integration (Synchronous)
- Products indexed on create/update
- Products removed from index on delete
- Circuit breaker prevents cascading failures

### Event Publishing (Kafka)
- `PRODUCT_CREATED` event on product creation
- `PRODUCT_UPDATED` event on product updates
- `PRODUCT_DELETED` event on product deletion
- Sequence numbers ensure ordering
- Event IDs prevent duplicates

---

## Troubleshooting

**Issue: JWT validation fails**
- Verify JWT_SECRET matches Auth Service configuration
- Check token expiration
- Verify token roles include ADMIN or SELLER

**Issue: Image validation fails**
- Ensure image URLs are accessible
- Check HTTP HEAD timeout (3 seconds)
- Use placeholder services like placeholder.com for testing

**Issue: Events not published**
- Verify Kafka is running on localhost:9092
- Check topic `product-events` exists
- Review application logs for publishing errors

**Issue: Database connection fails**
- Verify PostgreSQL is running
- Check port 5464 (or configured port)
- Verify database `catalog_db` exists

**Issue: Circuit breaker always open**
- Check Search Service availability
- Review circuit breaker configuration in application.yml
- Adjust failure threshold if needed for testing

---

## Using Swagger UI for Interactive Testing

The Catalog Service provides an interactive Swagger UI for testing API endpoints with JWT authentication.

### Accessing Swagger UI

1. **Start the catalog service** (as described in Prerequisites)

2. **Open Swagger UI in your browser:**
   ```
   http://localhost:8082/swagger-ui.html
   ```

3. **View OpenAPI specification (JSON):**
   ```
   http://localhost:8082/api-docs
   ```

### JWT Authentication in Swagger

#### Option 1: Basic Authentication (Easiest - Recommended)

The simplest way to authenticate in Swagger UI:

1. **Click the "Authorize" button** (lock icon at top right of Swagger UI)

2. **In the "Available authorizations" dialog:**
   - Find **"basicAuth (http, Basic)"** section
   - Enter your **email** in the **username** field: `admin@example.com`
   - Enter your **password**: `Admin123!`
   - Click **"Authorize"**
   - Click **"Close"**

3. **Done!** The system automatically converts your credentials to JWT tokens for all requests. No need to copy/paste tokens!

#### Option 2: Login Through Swagger UI Endpoint

Alternative method using the login endpoint:

1. **Find the Authentication section** in Swagger UI (should be at the top)

2. **Use the POST /api/v1/auth/login endpoint:**
   - Click "Try it out"
   - Enter credentials in the request body (use your email as username):
     ```json
     {
       "username": "admin@example.com",
       "password": "Admin123!"
     }
     ```
   - Click "Execute"
   - Copy the `accessToken` from the response

3. **Authorize with the token:**
   - Click the **"Authorize"** button (top right with a lock icon)
   - Find **"bearerAuth (http, Bearer)"** section
   - Paste the token (with or without "Bearer " prefix)
   - Click **"Authorize"**
   - Click **"Close"**

4. **All secured endpoints are now accessible!** The token persists across page refreshes.

#### Option 2: Get Token via Command Line

If you prefer using curl:

1. **Obtain JWT token from Auth Service:**
   ```bash
   # Login as ADMIN user
   curl -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{
       "username": "admin@marketplace.com",
       "password": "Admin@123"
     }'
   ```
   
   Copy the `accessToken` from the response.

2. **Authorize in Swagger UI:**
   - Click the **"Authorize"** button (top right, or lock icon on individual endpoints)
   - In the dialog, enter the token in one of these formats:
     - `Bearer <your-token-here>` (with "Bearer " prefix)
     - `<your-token-here>` (without prefix - Swagger adds "Bearer " automatically)
   - Click **"Authorize"** button
   - Click **"Close"** to dismiss the dialog

3. **Token is now active:**
   - All subsequent requests will include the JWT token
   - The authorization persists across page refreshes (see `persistAuthorization: true` in config)
   - Secured endpoints will now work without 401 Unauthorized errors

### Testing with Swagger UI

**Public Endpoints (No Authentication Required):**
- ✅ GET /api/v1/categories
- ✅ GET /api/v1/categories/{id}
- ✅ GET /api/v1/categories/top-level
- ✅ GET /api/v1/categories/{parentCategoryId}/subcategories
- ✅ GET /api/v1/products/{productId}
- ✅ GET /api/v1/products (list with filters)
- ✅ GET /api/v1/products/featured

**Secured Endpoints (Authentication Required):**
- 🔒 POST /api/v1/categories (ADMIN only)
- 🔒 DELETE /api/v1/categories/{id} (ADMIN only)
- 🔒 POST /api/v1/products (SELLER or ADMIN)
- 🔒 PUT /api/v1/products/{productId} (Owner or ADMIN)
- 🔒 DELETE /api/v1/products/{productId} (Owner or ADMIN)
- 🔒 GET /api/v1/products/seller/my-products (SELLER only)

**Testing Tips:**
1. **Try public endpoints first** without authentication to verify the API is working
2. **Test authentication** by trying a secured endpoint without a token (should get 401)
3. **Authenticate with correct role** for the endpoint you want to test
4. **Use example values** provided in the Swagger UI parameter descriptions
5. **Check response status codes** to understand success/failure reasons:
   - 200: Success
   - 201: Resource created
   - 204: Success with no content (e.g., DELETE)
   - 400: Bad request (validation error)
   - 401: Unauthorized (missing or invalid token)
   - 403: Forbidden (insufficient role)
   - 404: Resource not found
   - 409: Conflict (e.g., category name already exists)

### Creating Admin/Seller Users for Testing

If you need users with specific roles (ADMIN, SELLER), follow the instructions in the main testing guide under **"Generate Test JWT Tokens"** section. Remember:
- Auth Service doesn't allow setting roles during registration
- You must manually update the `user_roles` table in the user-service database
- Users must logout and login again after role changes to get updated JWT tokens

### Swagger UI Features

- **Filter endpoints:** Use the search box to filter operations
- **Try It Out:** Click "Try it out" on any endpoint to test it
- **Request/Response examples:** View example payloads and responses
- **Schema definitions:** Expand models to see detailed DTO structures
- **Persistent auth:** Token persists across page refreshes (convenient for testing)
