FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

# Build shared-dtos first
COPY common/shared-dtos/pom.xml ./common/shared-dtos/
COPY common/shared-dtos/src ./common/shared-dtos/src
RUN cd common/shared-dtos && mvn clean install -DskipTests -B

# Build payment-service
WORKDIR /app
COPY payment-service/pom.xml .
RUN mvn dependency:go-offline -B
COPY payment-service/src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S paymentservice && adduser -S paymentservice -G paymentservice -u 1005 \
    && chown -R paymentservice:paymentservice /app
USER paymentservice:1005

COPY --from=build /app/target/payment-service-*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
