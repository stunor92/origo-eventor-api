FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /build
COPY . .
RUN ./mvnw package -DskipTests

FROM gcr.io/distroless/java17-debian12
COPY --from=builder /build/target/*.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]