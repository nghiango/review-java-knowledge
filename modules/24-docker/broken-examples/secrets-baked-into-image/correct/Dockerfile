# syntax=docker/dockerfile:1.4
# ------------------------------------------------------------------------------
# Build stage using BuildKit secret mount (zero secret leakage into final layers)
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./
COPY src/ src/

# Access private token securely during build without persisting it in any layer
RUN --mount=type=secret,id=github_token \
    GITHUB_TOKEN=$(cat /run/secrets/github_token) && \
    ./gradlew bootJar --no-daemon -x test

# ------------------------------------------------------------------------------
# Runtime stage: Secrets provided at runtime via environment or mounted secrets
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder --chown=appuser:appgroup /build/build/libs/*.jar app.jar

USER appuser:appgroup

EXPOSE 8080

# Credentials must NOT be set via ENV; they are supplied at runtime via:
# - Kubernetes secretKeyRef
# - AWS Secrets Manager / Parameter Store
# - Docker Compose secrets
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
