# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

# First, build and install shared-dtos module
COPY common/shared-dtos/pom.xml ./common/shared-dtos/
COPY common/shared-dtos/src ./common/shared-dtos/src
RUN cd common/shared-dtos && mvn clean install -DskipTests -B

# Now build search-service
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY search-service/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY search-service/src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -g 1005 searchservice && \
    adduser -D -u 1005 -G searchservice searchservice

# Copy the built jar from build stage
COPY --from=build /app/target/search-service-*.jar app.jar

# Change ownership
RUN chown -R searchservice:searchservice /app

# Switch to non-root user
USER searchservice

# Expose port
EXPOSE 8085

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8085/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
