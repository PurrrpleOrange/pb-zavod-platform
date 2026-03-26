FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build
COPY . .
RUN mvn -pl gateway-app -am clean package -DskipTests
RUN echo "=== TARGET CONTENTS ===" && ls -la /build/gateway-app/target/

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/gateway-app/target/app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
