# Search Service

The Search Service provides fast, scalable product search functionality for the Digital Marketplace platform. It uses Elasticsearch for indexing and searching products, consuming events from the Catalog Service via Kafka for real-time index updates.

## Features

- **Full-text search**: Multi-match queries with field boosting (name^2, description)
- **Filtering**: By category, price range, status, seller, sizes, colors, and featured flag
- **Sorting**: By relevance (score), price, or newest (created date)
- **Faceted search**: Aggregations showing available filters with document counts
- **Autocomplete**: Completion suggester for product name suggestions
- **Pagination**: Configurable page size with validation (max 100 items)
- **Event-driven indexing**: Consumes Kafka events from Catalog Service for real-time updates
- **Distributed tracing**: Correlation ID propagation via X-Correlation-ID header

## Technology Stack

- **Spring Boot 3.2.1** with Java 21
- **Elasticsearch 8.11.1** for search and indexing
- **Spring Data Elasticsearch** for repository layer
- **Spring Kafka** for event consumption
- **Logstash Logback Encoder** for structured JSON logging
- **Springdoc OpenAPI** for API documentation

## Architecture

### Event-Driven Indexing

The Search Service maintains an Elasticsearch index synchronized with the Catalog Service through Kafka events:

```
Catalog Service → Kafka (product-events) → Search Service → Elasticsearch
```

**Event Types:**
- `ProductCreatedEvent`: Indexes new product
- `ProductUpdatedEvent`: Updates existing product document
- `ProductDeletedEvent`: Removes product from index

**Consumer Configuration:**
- Topic: `product-events`
- Consumer Group: `search-service-group`
- Concurrency: 1 (ensures ordering per product)
- Acknowledgment: Manual (commit only after successful indexing)

### Elasticsearch Index Mapping

```json
{
  "mappings": {
    "properties": {
      "productId": { "type": "keyword" },
      "name": {
        "type": "text",
        "fields": {
          "keyword": { "type": "keyword" },
          "suggest": { "type": "completion" }
        }
      },
      "description": { "type": "text" },
      "basePrice": { "type": "scaled_float", "scaling_factor": 100 },
      "categoryName": {
        "type": "text",
        "fields": { "keyword": { "type": "keyword" } }
      },
      "sellerId": { "type": "keyword" },
      "status": { "type": "keyword" },
      "availableSizes": { "type": "keyword" },
      "availableColors": { "type": "keyword" },
      "thumbnailUrl": { "type": "keyword" },
      "featured": { "type": "boolean" },
      "createdAt": { "type": "date" },
      "updatedAt": { "type": "date" }
    }
  }
}
```

## Quick Start

Get the Search Service up and running in minutes:

```bash
# 1. Start Elasticsearch
docker run -d \
  --name elasticsearch \
  -p 9200:9200 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  elasticsearch:8.11.1

# 2. Verify Elasticsearch is running
curl http://localhost:9200

# 3. Ensure Kafka is running (if using Docker Compose)
docker-compose up -d kafka zookeeper

# 4. Navigate to search-service directory
cd search-service

# 5. Build the service
mvn clean install

# 6. Run the service
mvn spring-boot:run

# 7. Access Swagger UI
open http://localhost:8085/swagger-ui.html
# Or visit: http://localhost:8085/swagger-ui.html

# 8. Test the health endpoint
curl http://localhost:8085/api/v1/search/health
```

The service will automatically:
- Create the `products` index in Elasticsearch on startup
- Start consuming events from the `product-events` Kafka topic
- Index products in real-time as they are created/updated in the Catalog Service

## Prerequisites

1. **Java 21** or higher
2. **Maven 3.6+**
3. **Elasticsearch 8.11.1** running on `localhost:9200`
4. **Apache Kafka** running on `localhost:9092`
5. **Catalog Service** running (for publishing product events)

## Setup Instructions

### 1. Start Elasticsearch

```bash
# Using Docker
docker run -d \
  --name elasticsearch \
  -p 9200:9200 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  elasticsearch:8.11.1

# Verify Elasticsearch is running
curl http://localhost:9200
```

### 2. Start Kafka (if not already running)

```bash
# Using Docker Compose (from project root)
docker-compose up -d kafka zookeeper

# Verify Kafka topic exists
kafka-topics --list --bootstrap-server localhost:9092
```

### 3. Build the Service

```bash
cd search-service
mvn clean install
```

### 4. Run the Service

```bash
mvn spring-boot:run
```

The service will start on **port 8085**.

### 5. Verify Startup

Check the logs for successful index creation:

```
INFO - Index 'products' created successfully
INFO - Started SearchServiceApplication
```

Verify the index was created:

```bash
curl http://localhost:9200/products
```

## API Endpoints

### Base URL
```
http://localhost:8085/api/v1/search
```

### Swagger UI
```
http://localhost:8085/swagger-ui.html
```

---

### 1. Search Products

**Endpoint:** `POST /api/v1/search/products`

**Description:** Search products with keyword, filters, sorting, and pagination.

**Request Body:**
```json
{
  "query": "laptop",
  "filters": {
    "categories": ["Electronics"],
    "priceRange": {
      "min": 100,
      "max": 1000
    },
    "statuses": ["ACTIVE"],
    "sellerId": "uuid-string",
    "sizes": ["M", "L"],
    "colors": ["Black", "Silver"],
    "featured": true
  },
  "sort": {
    "field": "PRICE",
    "direction": "ASC"
  },
  "page": 0,
  "size": 20
}
```

**Field Descriptions:**
- `query` (optional): Keyword to search in name and description
- `filters` (optional): Filter criteria
  - `categories`: List of category names
  - `priceRange`: Min/max price filter
  - `statuses`: Product statuses (e.g., "ACTIVE", "DRAFT")
  - `sellerId`: Filter by specific seller
  - `sizes`: Available sizes filter
  - `colors`: Available colors filter
  - `featured`: Filter featured products only
- `sort` (optional): Sorting options
  - `field`: RELEVANCE, PRICE, or NEWEST
  - `direction`: ASC or DESC
- `page`: Page number (0-indexed)
- `size`: Page size (max 100)

**Response:**
```json
{
  "products": [
    {
      "productId": "uuid",
      "name": "Gaming Laptop",
      "description": "High-performance gaming laptop",
      "basePrice": 999.99,
      "categoryName": "Electronics",
      "sellerId": "seller-uuid",
      "status": "ACTIVE",
      "availableSizes": ["15-inch", "17-inch"],
      "availableColors": ["Black", "Silver"],
      "thumbnailUrl": "https://example.com/image.jpg",
      "featured": true,
      "createdAt": "2025-01-15T10:00:00Z",
      "updatedAt": "2025-01-15T10:00:00Z",
      "score": 1.5
    }
  ],
  "totalResults": 42,
  "facets": {
    "categories": [
      {"value": "Electronics", "count": 25},
      {"value": "Computers", "count": 17}
    ],
    "priceRanges": [
      {"from": null, "to": 25.0, "count": 5, "label": "Under $25"},
      {"from": 25.0, "to": 50.0, "count": 10, "label": "$25 - $50"},
      {"from": 50.0, "to": 100.0, "count": 15, "label": "$50 - $100"},
      {"from": 100.0, "to": null, "count": 12, "label": "$100 and above"}
    ],
    "sizes": [
      {"value": "M", "count": 30},
      {"value": "L", "count": 25}
    ],
    "colors": [
      {"value": "Black", "count": 20},
      {"value": "Silver", "count": 15}
    ]
  },
  "pagination": {
    "currentPage": 0,
    "pageSize": 20,
    "totalPages": 3,
    "totalElements": 42,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

---

### 2. Get Autocomplete Suggestions

**Endpoint:** `GET /api/v1/search/suggestions?q={query}`

**Description:** Get product name suggestions based on partial input.

**Query Parameters:**
- `q`: Search query (required)

**Example:**
```bash
curl "http://localhost:8085/api/v1/search/suggestions?q=lapt"
```

**Response:**
```json
{
  "suggestions": [
    "Gaming Laptop",
    "Business Laptop",
    "Laptop Stand",
    "Laptop Bag"
  ],
  "count": 4
}
```

---

### 3. Health Check

**Endpoint:** `GET /api/v1/search/health`

**Description:** Check if the search service is running.

**Response:**
```json
{
  "status": "UP",
  "service": "search-service"
}
```

---

## Testing the Service

### Step 1: Start Required Services

Make sure the following services are running:

1. **Elasticsearch** on `localhost:9200`
2. **Kafka** on `localhost:9092`
3. **Catalog Service** on `localhost:8082`
4. **Search Service** on `localhost:8085`

### Step 2: Create Products in Catalog Service

The Search Service indexes products by consuming events from the Catalog Service. First, create some products:

```bash
# Create a product
curl -X POST http://localhost:8082/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Gaming Laptop",
    "description": "High-performance gaming laptop with RTX 4090",
    "basePrice": 1999.99,
    "categoryId": "<category-uuid>",
    "sellerId": "<seller-uuid>",
    "imageUrls": ["https://example.com/laptop.jpg"],
    "availableSizes": ["15-inch", "17-inch"],
    "availableColors": ["Black", "Silver"]
  }'
```

**Note:** The Catalog Service will emit a `ProductCreatedEvent` to Kafka, which the Search Service will consume and index the product in Elasticsearch.

### Step 3: Verify Product Indexed

Wait a few seconds for the event to be consumed and indexed, then check the Elasticsearch index:

```bash
# Check all indexed products
curl "http://localhost:9200/products/_search?pretty"
```

### Step 4: Test Search Endpoints

#### Basic Keyword Search

```bash
curl -X POST http://localhost:8085/api/v1/search/products \
  -H "Content-Type: application/json" \
  -d '{
    "query": "laptop",
    "page": 0,
    "size": 20
  }'
```

#### Search with Filters

```bash
curl -X POST http://localhost:8085/api/v1/search/products \
  -H "Content-Type: application/json" \
  -d '{
    "query": "laptop",
    "filters": {
      "categories": ["Electronics"],
      "priceRange": {
        "min": 500,
        "max": 2500
      },
      "statuses": ["ACTIVE"]
    },
    "sort": {
      "field": "PRICE",
      "direction": "ASC"
    },
    "page": 0,
    "size": 10
  }'
```

#### Search by Category Only

```bash
curl -X POST http://localhost:8085/api/v1/search/products \
  -H "Content-Type: application/json" \
  -d '{
    "filters": {
      "categories": ["Electronics"]
    },
    "page": 0,
    "size": 20
  }'
```

#### Search Featured Products

```bash
curl -X POST http://localhost:8085/api/v1/search/products \
  -H "Content-Type: application/json" \
  -d '{
    "filters": {
      "featured": true
    },
    "sort": {
      "field": "NEWEST",
      "direction": "DESC"
    },
    "page": 0,
    "size": 10
  }'
```

#### Get Suggestions

```bash
curl "http://localhost:8085/api/v1/search/suggestions?q=gam"
```

### Step 5: Test Event Consumption

#### Update a Product

```bash
curl -X PUT http://localhost:8082/api/v1/products/<product-id> \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Gaming Laptop",
    "description": "High-performance gaming laptop with RTX 5090",
    "basePrice": 2499.99,
    "categoryId": "<category-uuid>",
    "sellerId": "<seller-uuid>",
    "imageUrls": ["https://example.com/laptop-new.jpg"],
    "availableSizes": ["15-inch", "17-inch", "19-inch"],
    "availableColors": ["Black", "Silver", "White"]
  }'
```

The Search Service will consume the `ProductUpdatedEvent` and update the Elasticsearch document.

#### Delete a Product

```bash
curl -X DELETE http://localhost:8082/api/v1/products/<product-id>
```

The Search Service will consume the `ProductDeletedEvent` and remove the document from Elasticsearch.

### Step 6: Verify Correlation ID Tracing

```bash
curl -X POST http://localhost:8085/api/v1/search/products \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: test-correlation-123" \
  -d '{"query": "laptop"}' \
  -v
```

Check the response headers for the `X-Correlation-ID` header, and verify it appears in the logs.

---

## Monitoring and Logging

### Logs Location

Logs are written to:
- **Console**: JSON-formatted with correlation IDs
- **File**: `logs/search-service.log` (rotates daily, 30-day retention)

### Log Format

```json
{
  "timestamp": "2025-01-15T10:30:45.123Z",
  "level": "INFO",
  "service": "search-service",
  "correlationId": "abc-123-def",
  "logger": "com.marketplace.search.service.SearchService",
  "message": "Search completed: 42 results found"
}
```

### Kafka Consumer Monitoring

Monitor consumer lag and offset:

```bash
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group search-service-group \
  --describe
```

### Elasticsearch Index Stats

```bash
# Get index statistics
curl "http://localhost:9200/products/_stats?pretty"

# Get document count
curl "http://localhost:9200/products/_count?pretty"

# Check index health
curl "http://localhost:9200/_cluster/health/products?pretty"
```

---

## Configuration

Key configuration properties in `application.yml`:

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200

  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: search-service-group
      auto-offset-reset: earliest
      enable-auto-commit: false

server:
  port: 8085

search:
  index:
    name: products
    number-of-shards: 1
    number-of-replicas: 1
  pagination:
    default-page-size: 20
    max-page-size: 100
  kafka:
    topic: product-events
```

---

## Troubleshooting

### Issue: Index not created on startup

**Solution:** Check Elasticsearch connection:
```bash
curl http://localhost:9200
```

Verify the service has permission to create indices.

### Issue: Events not being consumed

**Solution:**
1. Verify Kafka topic exists:
   ```bash
   kafka-topics --list --bootstrap-server localhost:9092
   ```
2. Check consumer group status:
   ```bash
   kafka-consumer-groups --bootstrap-server localhost:9092 \
     --group search-service-group --describe
   ```
3. Check Catalog Service is emitting events
4. Review Search Service logs for consumer errors

### Issue: Search returns no results

**Solution:**
1. Verify products are indexed:
   ```bash
   curl "http://localhost:9200/products/_count"
   ```
2. Check product status filter (only ACTIVE products may be visible)
3. Review query syntax and filters
4. Check Elasticsearch logs

### Issue: Slow search performance

**Solution:**
1. Add more Elasticsearch shards/replicas
2. Optimize query (reduce filters, use keyword fields)
3. Implement caching for common queries
4. Consider pagination with `search_after` for deep pagination

---

## Architecture Patterns

### Event-Driven Architecture
- Decoupled from Catalog Service via Kafka
- At-least-once delivery with manual acknowledgment
- Idempotent indexing (Elasticsearch save is upsert)

### Multi-Field Mapping
- Text fields for full-text search
- Keyword fields for exact matching and aggregations
- Completion suggester for autocomplete

### Field Boosting
- Product name boosted 2x for better relevance
- Description has default weight

### Faceted Search
- Dynamic aggregations showing available filters
- Counts updated based on current search results

### Correlation ID Tracing
- Propagated via X-Correlation-ID header
- Stored in MDC for all log entries
- Returned in response headers

---

## Future Enhancements

- Bulk reindexing endpoint (fetch all products from Catalog Service)
- Advanced search features (spell correction, synonym expansion, personalized ranking)
- Search analytics (query logging, popular searches, conversion tracking)
- Performance optimizations (result caching, search_after for deep pagination)
- Resilience (circuit breaker for Elasticsearch, dead letter queue for failed events)
- Security (API authentication/authorization)
- Multi-language support (language-specific analyzers)
- Geo-search (location-based product search)
- Index lifecycle management (rollover, snapshots)

---

## API Documentation

Full API documentation is available via Swagger UI:

**URL:** http://localhost:8085/swagger-ui.html

---

## License

Copyright © 2025 Digital Marketplace. All rights reserved.
