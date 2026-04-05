# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
# ↑ Download dependencies first as a separate layer.
#   If only source code changes, this layer is cached and not re-downloaded.
COPY src ./src
RUN mvn package -DskipTests -q
# ↑ -DskipTests: don't run tests during Docker build (we run them separately)

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]