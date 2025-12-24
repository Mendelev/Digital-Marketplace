FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

# Build shared-dtos first
COPY common/shared-dtos/pom.xml ./common/shared-dtos/
COPY common/shared-dtos/src ./common/shared-dtos/src
RUN cd common/shared-dtos && mvn clean install -DskipTests -B

# Build cart-service
WORKDIR /app
COPY cart-service/pom.xml .
RUN mvn dependency:go-offline -B
COPY cart-service/src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S cartservice && adduser -S cartservice -G cartservice -u 1003 \
    && mkdir -p /app/logs \
    && chown -R cartservice:cartservice /app
USER cartservice:1003

COPY --from=build /app/target/cart-service-*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
