FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

# Build shared-dtos first
COPY common/shared-dtos/pom.xml ./common/shared-dtos/
COPY common/shared-dtos/src ./common/shared-dtos/src
RUN cd common/shared-dtos && mvn clean install -DskipTests -B

# Build inventory-service
WORKDIR /app
COPY inventory-service/pom.xml .
RUN mvn dependency:go-offline -B
COPY inventory-service/src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S inventoryservice && adduser -S inventoryservice -G inventoryservice -u 1006 \
    && chown -R inventoryservice:inventoryservice /app
USER inventoryservice:1006

COPY --from=build /app/target/inventory-service-*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
