FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /build
COPY . .
RUN ./gradlew shadowJar --no-daemon

FROM gcr.io/distroless/java17-debian12
COPY --from=builder /build/build/libs/*.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]