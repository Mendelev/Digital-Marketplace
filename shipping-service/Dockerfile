FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

# Build shared-dtos first
COPY common/shared-dtos/pom.xml ./common/shared-dtos/
COPY common/shared-dtos/src ./common/shared-dtos/src
RUN cd common/shared-dtos && mvn clean install -DskipTests -B

# Build shipping-service
WORKDIR /app
COPY shipping-service/pom.xml .
RUN mvn dependency:go-offline -B
COPY shipping-service/src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S shippingservice && adduser -S shippingservice -G shippingservice -u 1006 \
    && chown -R shippingservice:shippingservice /app
USER shippingservice:1006

COPY --from=build /app/target/shipping-service-*.jar app.jar

EXPOSE 8088

ENTRYPOINT ["java", "-jar", "app.jar"]
