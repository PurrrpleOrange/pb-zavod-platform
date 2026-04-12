FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build
COPY . .
RUN mvn -pl catalog-app -am clean package -DskipTests
RUN test -f /build/catalog-app/target/app.jar
RUN cd /build && jar xf /build/catalog-app/target/app.jar META-INF/MANIFEST.MF && grep -q "^Main-Class:" META-INF/MANIFEST.MF

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/catalog-app/target/app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
