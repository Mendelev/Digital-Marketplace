FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

# Build shared-dtos first
COPY common/shared-dtos/pom.xml ./common/shared-dtos/
COPY common/shared-dtos/src ./common/shared-dtos/src
RUN cd common/shared-dtos && mvn clean install -DskipTests -B

# Build order-service
WORKDIR /app
COPY order-service/pom.xml .
RUN mvn dependency:go-offline -B
COPY order-service/src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S orderservice && adduser -S orderservice -G orderservice -u 1004 \
    && mkdir -p /app/logs \
    && chown -R orderservice:orderservice /app
USER orderservice:1004

COPY --from=build /app/target/order-service-*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]
