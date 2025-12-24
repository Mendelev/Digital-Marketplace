#!/bin/bash

# Quick Start Script for Shipping Service
# This script sets up and starts the shipping service with PostgreSQL

set -e  # Exit on error

echo "=================================================="
echo "  Shipping Service - Quick Start Setup"
echo "=================================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored messages
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi
print_success "Docker is running"

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose not found. Please install docker-compose."
    exit 1
fi
print_success "docker-compose is available"

echo ""
print_info "Starting services..."
echo ""

# Stop any existing containers
docker-compose down 2>/dev/null || true

# Build and start services
print_info "Building shipping service image..."
docker-compose build

print_info "Starting PostgreSQL and Shipping Service..."
docker-compose up -d

echo ""
print_info "Waiting for services to be healthy..."
echo ""

# Wait for PostgreSQL
MAX_WAIT=60
WAIT_COUNT=0
until docker-compose exec -T postgres pg_isready -U shipping_user -d shipping_db > /dev/null 2>&1; do
    if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
        print_error "PostgreSQL failed to start within ${MAX_WAIT} seconds"
        docker-compose logs postgres
        exit 1
    fi
    echo -n "."
    sleep 1
    ((WAIT_COUNT++))
done
print_success "PostgreSQL is ready"

# Wait for Shipping Service
WAIT_COUNT=0
until curl -f http://localhost:8089/actuator/health > /dev/null 2>&1; do
    if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
        print_error "Shipping Service failed to start within ${MAX_WAIT} seconds"
        echo ""
        print_info "Last 50 log lines:"
        docker-compose logs --tail 50 shipping-service
        exit 1
    fi
    echo -n "."
    sleep 2
    ((WAIT_COUNT++))
done
echo ""
print_success "Shipping Service is ready"

echo ""
echo "=================================================="
print_success "Shipping Service is running!"
echo "=================================================="
echo ""
echo "Service Information:"
echo "  - Shipping Service: http://localhost:8089"
echo "  - Swagger UI: http://localhost:8089/swagger-ui.html"
echo "  - API Docs: http://localhost:8089/api-docs"
echo "  - Health Check: http://localhost:8089/actuator/health"
echo "  - PostgreSQL: localhost:5434"
echo ""
echo "Database Information:"
echo "  - Database: shipping_db"
echo "  - User: shipping_user"
echo "  - Password: shipping_pass"
echo ""
echo "Useful Commands:"
echo "  - View logs: docker-compose logs -f shipping-service"
echo "  - Stop services: docker-compose down"
echo "  - Restart: docker-compose restart shipping-service"
echo "  - Access database: docker-compose exec postgres psql -U shipping_user -d shipping_db"
echo ""
echo "Testing:"
echo "  - See TESTING.md for comprehensive test scenarios"
echo "  - Quick test: curl http://localhost:8089/actuator/health"
echo ""
print_info "To view logs, run: docker-compose logs -f shipping-service"
echo ""
