FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy Maven build
COPY target/payment-service-1.0.0-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8083

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
