FROM eclipse-temurin:25-jdk-jammy AS builder
WORKDIR /build
COPY . .
RUN ./mvnw package -DskipTests

FROM gcr.io/distroless/java25-debian13
COPY --from=builder /build/target/*.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]