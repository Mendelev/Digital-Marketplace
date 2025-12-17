# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

# First, build and install shared-dtos module
COPY common/shared-dtos/pom.xml ./common/shared-dtos/
COPY common/shared-dtos/src ./common/shared-dtos/src
RUN cd common/shared-dtos && mvn clean install -DskipTests -B

# Now build user-service
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY user-service/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY user-service/src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 userservice && \
    adduser -D -u 1001 -G userservice userservice

# Copy the built jar from build stage
COPY --from=build /app/target/user-service-*.jar app.jar

# Change ownership
RUN chown -R userservice:userservice /app

# Switch to non-root user
USER userservice

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
