# Docker Deployment Guide

This guide explains how to run the Auth Service using Docker containers.

## Files

- **Dockerfile** - Multi-stage build for the Auth Service
- **docker-compose.yml** - Infrastructure only (PostgreSQL + Mockoon)
- **docker-compose-full.yml** - Complete stack (PostgreSQL + Mockoon + Auth Service)
- **.dockerignore** - Files to exclude from Docker build context

## Quick Start

### Option 1: Run Everything with Docker (Recommended for Production-like Testing)

Run the complete stack including the Auth Service:

```bash
# Build and start all services
docker-compose -f docker-compose-full.yml up --build

# Or run in detached mode
docker-compose -f docker-compose-full.yml up --build -d

# View logs
docker-compose -f docker-compose-full.yml logs -f auth-service

# Stop services
docker-compose -f docker-compose-full.yml down
```

The Auth Service will be available at: http://localhost:8080

### Option 2: Infrastructure Only (For Local Development)

Run only PostgreSQL and Mockoon, develop the Auth Service locally:

```bash
# Start infrastructure
docker-compose up -d

# Run migrations
mvn flyway:migrate

# Run Auth Service locally
mvn spring-boot:run
```

## Service URLs

| Service | URL | Description |
|---------|-----|-------------|
| Auth Service | http://localhost:8080 | Main application |
| Swagger UI | http://localhost:8080/swagger-ui.html | API documentation |
| PostgreSQL | localhost:5462 | Database (mapped from container port 5432) |
| Mockoon (User Service) | http://localhost:3001 | User Service mock |

## Database Access

Access PostgreSQL running in Docker:

```bash
# Connect to database
docker exec -it auth-service-postgres psql -U auth_user -d auth_db

# Run a query
docker exec -it auth-service-postgres psql -U auth_user -d auth_db -c "SELECT * FROM credentials;"
```

## Running Flyway Migrations

### With Docker Compose Full Stack

Migrations run automatically when the Auth Service container starts.

### With Local Development

```bash
mvn flyway:migrate
```

## Building the Docker Image

Build the Auth Service image manually:

```bash
# Build with default tag
docker build -t auth-service:latest .

# Build with specific tag
docker build -t auth-service:1.0.0 .
```

## Environment Variables

The Auth Service container accepts these environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| SPRING_DATASOURCE_URL | jdbc:postgresql://postgres:5432/auth_db | Database URL |
| SPRING_DATASOURCE_USERNAME | auth_user | Database username |
| SPRING_DATASOURCE_PASSWORD | auth_pass | Database password |
| USER_SERVICE_BASE_URL | http://mockoon:3001 | User Service URL |
| JWT_PRIVATE_KEY_PATH | file:/app/keys/private_key.pem | Private key path |
| JWT_PUBLIC_KEY_PATH | file:/app/keys/public_key.pem | Public key path |
| LOGGING_LEVEL_COM_MARKETPLACE_AUTH | INFO | Logging level |

## Health Checks

Both Docker Compose configurations include health checks:

**PostgreSQL:**
- Endpoint: `pg_isready`
- Interval: 10s
- Timeout: 5s
- Retries: 5

**Auth Service:**
- Endpoint: http://localhost:8080/actuator/health
- Interval: 30s
- Timeout: 10s
- Start period: 60s
- Retries: 5

**Mockoon:**
- Endpoint: http://localhost:3001/health
- Interval: 10s
- Timeout: 5s
- Retries: 5

Check service health:

```bash
# All services
docker-compose -f docker-compose-full.yml ps

# Specific service logs
docker-compose -f docker-compose-full.yml logs auth-service
```

## Testing with Docker

### Setup Test User

```bash
# Insert test credential
docker exec -it auth-service-postgres psql -U auth_user -d auth_db -c "
INSERT INTO credentials (id, user_id, email, password_hash, status, failed_login_count, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890'::uuid,
    'test@example.com',
    '\$2y\$12\$9G3CBMX/HdtzZg95v8FDqeR8WKJFCSiK2t9Sc6V8G370VizzdA8qW',
    'ACTIVE',
    0,
    NOW(),
    NOW()
);"
```

### Test Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

## Troubleshooting

### Service won't start

Check logs:
```bash
docker-compose -f docker-compose-full.yml logs auth-service
```

### Database connection issues

Verify PostgreSQL is running:
```bash
docker-compose -f docker-compose-full.yml ps postgres
docker-compose -f docker-compose-full.yml logs postgres
```

### RSA keys missing

The keys should be in `src/main/resources/keys/`. If missing, generate them:
```bash
mvn compile exec:java -Dexec.mainClass="com.marketplace.auth.util.KeyPairGeneratorUtil"
```

Then rebuild the Docker image:
```bash
docker-compose -f docker-compose-full.yml up --build
```

### Check if Flyway migrations ran

```bash
docker exec -it auth-service-postgres psql -U auth_user -d auth_db -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank;"
```

### Port conflicts

If ports 8080, 5462, or 3001 are already in use, edit `docker-compose-full.yml`:

```yaml
ports:
  - "8081:8080"  # Change host port
```

## Cleanup

Remove all containers and volumes:

```bash
# Stop and remove containers (keeps volumes)
docker-compose -f docker-compose-full.yml down

# Stop and remove containers + volumes
docker-compose -f docker-compose-full.yml down -v

# Remove images
docker rmi auth-service:latest
```

## Production Considerations

For production deployment:

1. **Use external database** - Don't run PostgreSQL in the same compose file
2. **Set proper secrets** - Use Docker secrets or environment variables
3. **Configure resource limits** - Add memory and CPU limits
4. **Use image registry** - Push to Docker Hub or private registry
5. **Enable TLS** - Configure HTTPS with proper certificates
6. **Backup RSA keys** - Store keys securely outside containers
7. **Configure logging** - Send logs to external logging system
8. **Set restart policy** - Use `restart: always` for production
9. **Health monitoring** - Integrate with monitoring tools
10. **Update base images** - Regularly update Java and Alpine versions

## Example Production Compose Snippet

```yaml
auth-service:
  image: your-registry/auth-service:1.0.0
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://prod-db-host:5432/auth_db
  deploy:
    resources:
      limits:
        cpus: '1.0'
        memory: 512M
      reservations:
        cpus: '0.5'
        memory: 256M
  restart: always
  logging:
    driver: "json-file"
    options:
      max-size: "10m"
      max-file: "3"
```
