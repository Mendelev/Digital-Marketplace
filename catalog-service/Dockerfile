FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

# Build shared-dtos first
COPY common/shared-dtos/pom.xml ./common/shared-dtos/
COPY common/shared-dtos/src ./common/shared-dtos/src
RUN cd common/shared-dtos && mvn clean install -DskipTests -B

# Build catalog-service
WORKDIR /app
COPY catalog-service/pom.xml .
RUN mvn dependency:go-offline -B
COPY catalog-service/src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S catalogservice && adduser -S catalogservice -G catalogservice -u 1002
USER catalogservice:1002

COPY --from=build /app/target/catalog-service-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
