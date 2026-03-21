FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build
COPY . .
RUN mvn -pl auth-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/auth-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
