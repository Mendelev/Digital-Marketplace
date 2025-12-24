# Shipping Service - Troubleshooting Guide

## Common Issues and Solutions

### Issue 1: PostgreSQL "role root does not exist"

**Problem:**
```
createdb: error: connection to server on socket "/var/run/postgresql/.s.PGSQL.5432" failed: FATAL:  role "root" does not exist
```

**Cause:** You're running PostgreSQL commands as root user instead of postgres user.

**Solution:**
```bash
# WRONG: Running as root
docker exec -it postgres-container createdb shipping_db

# CORRECT: Running as postgres user
docker exec -it postgres-container psql -U postgres -c "CREATE DATABASE shipping_db;"

# Or enter the container and switch to postgres user
docker exec -it postgres-container bash
su - postgres  # Switch to postgres user
psql
CREATE DATABASE shipping_db;
CREATE USER shipping_user WITH PASSWORD 'shipping_pass';
GRANT ALL PRIVILEGES ON DATABASE shipping_db TO shipping_user;
\c shipping_db
GRANT ALL ON SCHEMA public TO shipping_user;
\q
```

### Issue 2: Shipping Service Container Won't Start

**Symptoms:**
- Container keeps restarting
- Logs show connection errors
- Health check failing

**Diagnostic Steps:**

1. **Check container logs:**
```bash
docker logs shipping-service -f
```

2. **Common error patterns and fixes:**

#### Error: "Unable to connect to database"
```
org.postgresql.util.PSQLException: Connection refused
```

**Solutions:**
- Ensure PostgreSQL is running: `docker ps | grep postgres`
- Check database URL in docker-compose.yml
- If using `host.docker.internal`, ensure Docker Desktop is configured correctly
- Use the PostgreSQL service name instead: `jdbc:postgresql://postgres:5432/shipping_db`

#### Error: "Database 'shipping_db' does not exist"
```
org.postgresql.util.PSQLException: FATAL: database "shipping_db" does not exist
```

**Solution:**
```bash
# Create database
docker exec -it <postgres-container> psql -U postgres -c "CREATE DATABASE shipping_db;"
```

#### Error: "Permission denied for schema public"
```
org.postgresql.util.PSQLException: ERROR: permission denied for schema public
```

**Solution (PostgreSQL 15+):**
```bash
docker exec -it <postgres-container> psql -U postgres -d shipping_db -c "GRANT ALL ON SCHEMA public TO shipping_user;"
docker exec -it <postgres-container> psql -U postgres -d shipping_db -c "GRANT CREATE ON SCHEMA public TO shipping_user;"
```

#### Error: "Cannot connect to Kafka"
```
org.apache.kafka.common.errors.TimeoutException: Failed to update metadata
```

**Solutions:**
- Ensure Kafka is running: `docker ps | grep kafka`
- Check KAFKA_BROKERS environment variable
- If Kafka is on host: Use `host.docker.internal:9092`
- If Kafka is in Docker: Use container name, e.g., `kafka:9092`

#### Error: "Auth Service unavailable"
```
Auth Service unavailable and no cached key available
```

**Solutions:**
- Start Auth Service first
- Check AUTH_SERVICE_URL is correct
- Verify network connectivity: `docker exec shipping-service ping auth-service`

### Issue 3: Container Starts Then Crashes

**Check startup logs:**
```bash
docker logs shipping-service --tail 100
```

**Common causes:**

1. **Out of Memory:**
```bash
# Increase memory in docker-compose.yml
environment:
  JAVA_OPTS: "-Xmx1024m -Xms512m"  # Increase from default
```

2. **Port already in use:**
```bash
# Check what's using port 8088
lsof -i :8088

# Change external port in docker-compose.yml
ports:
  - "8089:8088"  # Use different external port
```

3. **Flyway migration failures:**
```bash
# Check if Flyway baseline is needed
docker exec -it <postgres-container> psql -U postgres -d shipping_db

# Inside psql:
\dt  # List tables
# If tables exist, you may need to baseline:
# Set spring.flyway.baseline-on-migrate=true in application.yml (already set)
```

### Issue 4: Health Check Failing

**Symptoms:**
```bash
docker ps
# Shows status as "unhealthy"
```

**Solutions:**

1. **Check health endpoint manually:**
```bash
docker exec shipping-service wget -O- http://localhost:8088/actuator/health
# or
docker exec shipping-service wget -O- http://localhost:8088/api-docs
```

2. **Increase health check timing:**
```yaml
# In docker-compose.yml
healthcheck:
  start_period: 120s  # Increase from 60s
  interval: 30s
  timeout: 10s
  retries: 5
```

3. **Check if service is actually running:**
```bash
docker exec shipping-service ps aux | grep java
```

### Issue 5: Events Not Being Consumed

**Symptoms:**
- OrderConfirmed events published but shipments not created
- No errors in logs

**Diagnostic Steps:**

1. **Verify Kafka topic exists:**
```bash
kafka-topics --list --bootstrap-server localhost:9092 | grep order-events
```

2. **Check consumer group:**
```bash
kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group shipping-service
```

3. **Monitor Kafka topic:**
```bash
kafka-console-consumer --bootstrap-server localhost:9092 --topic order-events --from-beginning
```

4. **Check Spring Cloud Stream bindings in logs:**
```bash
docker logs shipping-service | grep "Binding"
```

Should see:
```
Binding orderEvents-in-0 to order-events
Binding shippingEvents-out-0 to shipping-events
```

5. **Verify consumer configuration:**
```yaml
# In application.yml - check:
spring.cloud.stream.bindings.orderEvents-in-0.destination: order-events
spring.cloud.stream.bindings.orderEvents-in-0.group: shipping-service
```

## Complete Docker Setup

### Recommended docker-compose.yml

Use the provided `docker-compose.yml` which includes PostgreSQL:

```bash
cd shipping-service
docker-compose up -d
```

This automatically:
- ✅ Creates PostgreSQL with correct database and user
- ✅ Sets up networking between services
- ✅ Waits for PostgreSQL to be healthy before starting app
- ✅ Configures health checks
- ✅ Sets up persistent volumes

### Full Stack Setup (All Services)

If running all services together:

```bash
# 1. Start infrastructure (PostgreSQL, Kafka, Zookeeper)
docker-compose -f docker-compose.postgres.yml up -d
docker-compose -f docker-compose.kafka.yml up -d  # If you have this

# 2. Start auth service
cd auth-service && docker-compose -f docker-compose-full.yml up -d && cd ..

# 3. Start order service
cd order-service && docker-compose -f docker-compose-full.yml up -d && cd ..

# 4. Start shipping service
cd shipping-service && docker-compose up -d && cd ..
```

## Verification Steps

### 1. Check Container Status
```bash
docker ps -a | grep shipping
```
Should show status as "Up" and "healthy"

### 2. Check Logs
```bash
docker logs shipping-service --tail 50
```
Should show:
```
Started ShippingServiceApplication in X.XXX seconds
```

### 3. Test Health Endpoint
```bash
curl http://localhost:8089/actuator/health
```
Should return:
```json
{"status":"UP"}
```

### 4. Test API Docs
```bash
curl http://localhost:8089/api-docs
```
Should return OpenAPI JSON spec

### 5. Test Database Connection
```bash
docker exec shipping-service wget -O- http://localhost:8088/actuator/health | grep db
```

### 6. Test Service Creation
```bash
curl -X POST http://localhost:8089/api/v1/shipments \
  -H "Content-Type: application/json" \
  -H "X-Service-Secret: dev-secret-change-in-production" \
  -d '{
    "orderId": "660e8400-e29b-41d4-a716-446655440001",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "shippingAddress": {
      "street": "123 Main St",
      "city": "San Francisco",
      "state": "CA",
      "zip": "94105",
      "country": "USA"
    },
    "itemCount": 1
  }'
```

## Clean Restart

If all else fails, clean restart:

```bash
# Stop and remove containers
docker-compose down -v

# Remove images (optional)
docker rmi shipping-service shipping-postgres

# Rebuild and start
docker-compose build --no-cache
docker-compose up -d

# Watch logs
docker-compose logs -f
```

## Getting Help

If issues persist:

1. **Collect diagnostic information:**
```bash
# Container status
docker ps -a | grep shipping

# Recent logs
docker logs shipping-service --tail 200 > shipping-logs.txt

# Docker inspect
docker inspect shipping-service > shipping-inspect.txt

# Environment variables
docker exec shipping-service env | grep -E "(SPRING|KAFKA|AUTH|ORDER|DB)"

# Java process
docker exec shipping-service ps aux | grep java
```

2. **Check specific subsystems:**
```bash
# Database connectivity
docker exec shipping-service nc -zv postgres 5432

# Kafka connectivity
docker exec shipping-service nc -zv kafka 9092

# Auth service connectivity
docker exec shipping-service curl -v http://auth-service:8080/api/v1/auth/public-key
```

3. **Review configurations:**
- Check environment variables in docker-compose.yml
- Verify application.yml settings
- Confirm network configuration
- Check volume mounts

## Quick Reference

### PostgreSQL Commands
```bash
# Connect to database
docker exec -it <postgres-container> psql -U shipping_user -d shipping_db

# List tables
\dt

# Check migrations
SELECT * FROM flyway_schema_history;

# Check shipments
SELECT * FROM shipments LIMIT 5;
```

### Docker Commands
```bash
# View logs
docker logs shipping-service -f

# Restart service
docker restart shipping-service

# Execute command in container
docker exec -it shipping-service bash

# Check health
docker inspect shipping-service | grep -A 10 Health
```

### Network Debugging
```bash
# Test PostgreSQL connection
docker exec shipping-service nc -zv postgres 5432

# Test Kafka connection
docker exec shipping-service nc -zv kafka 9092

# Test Auth Service
docker exec shipping-service curl http://auth-service:8080/actuator/health
```
