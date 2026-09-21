# syntax=docker/dockerfile:1
# ------------------------------------------------------------------------------
# Stage 1: Build & Layer Extraction
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy dependency manifests first to leverage Docker layer caching
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

# Download dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code and build production jar
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# Extract Spring Boot Layered JAR
WORKDIR /build/layers
RUN java -Djarmode=layertools -jar /build/build/libs/*.jar extract

# ------------------------------------------------------------------------------
# Stage 2: Minimal Production Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Create unprivileged system user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy layers in order of change frequency (least frequent to most frequent)
COPY --from=builder --chown=appuser:appgroup /build/layers/dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/layers/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appgroup /build/layers/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/layers/application/ ./

# Run as non-root user
USER appuser:appgroup

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseG1GC", "org.springframework.boot.loader.launch.JarLauncher"]
