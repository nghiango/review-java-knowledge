# Docker Exercises

Practical hands-on challenges to reinforce container image optimization, layered JAR packaging, and vulnerability scanning.

---

## Exercise 1: Build an Optimized Spring Boot Layered Dockerfile

### Problem Statement

Given a Spring Boot 3.5 application built with Gradle, create a production-grade multi-stage `Dockerfile` that:
1. Builds the application using `eclipse-temurin:21-jdk-alpine`.
2. Maximizes Docker build cache hit rates by copying build descriptors and downloading dependencies before copying source code.
3. Unpacks the Spring Boot executable JAR using `layertools`.
4. Copies the layers into an unprivileged runtime image based on `eclipse-temurin:21-jre-alpine` running as `appuser:appgroup` (UID 10001).
5. Configures container memory ergonomics using `-XX:MaxRAMPercentage=75.0`.
6. Attaches an Actuator liveness `HEALTHCHECK` with a 25-second startup grace window.
7. Executes the application via Spring Boot's `JarLauncher` in exec form.

??? tip "Reveal Solution"
    ```dockerfile
    # syntax=docker/dockerfile:1
    # ------------------------------------------------------------------------------
    # Stage 1: Build & Layer Extraction
    # ------------------------------------------------------------------------------
    FROM eclipse-temurin:21-jdk-alpine AS builder

    WORKDIR /workspace

    # Copy dependency manifests first for build caching
    COPY gradlew .
    COPY gradle/ gradle/
    COPY build.gradle.kts settings.gradle.kts gradle.properties ./

    # Cache dependencies
    RUN ./gradlew dependencies --no-daemon || true

    # Copy application source and build fat JAR
    COPY src/ src/
    RUN ./gradlew bootJar --no-daemon -x test

    # Extract layered JAR
    WORKDIR /workspace/extracted
    RUN java -Djarmode=layertools -jar /workspace/build/libs/*.jar extract

    # ------------------------------------------------------------------------------
    # Stage 2: Production Runtime
    # ------------------------------------------------------------------------------
    FROM eclipse-temurin:21-jre-alpine AS runner

    WORKDIR /app

    # Create unprivileged system user
    RUN addgroup -S appgroup -g 10001 && \
        adduser -S appuser -u 10001 -G appgroup

    # Copy layers in ascending frequency of modification
    COPY --from=builder --chown=appuser:appgroup /workspace/extracted/dependencies/ ./
    COPY --from=builder --chown=appuser:appgroup /workspace/extracted/spring-boot-loader/ ./
    COPY --from=builder --chown=appuser:appgroup /workspace/extracted/snapshot-dependencies/ ./
    COPY --from=builder --chown=appuser:appgroup /workspace/extracted/application/ ./

    USER appuser:appgroup

    EXPOSE 8080

    HEALTHCHECK --interval=15s --timeout=3s --start-period=25s --retries=3 \
      CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

    ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseG1GC", "org.springframework.boot.loader.launch.JarLauncher"]
    ```

---

## Exercise 2: Automated Container Vulnerability Scanning with Trivy

### Problem Statement

Design a CI/CD workflow step or script that scans your generated Docker image for known Common Vulnerabilities and Exposures (CVEs) before pushing to an enterprise registry.

The scanner must:
1. Scan both OS packages (Alpine/Debian) and Java application dependencies (JARs).
2. Fail the build (exit code 1) if any **CRITICAL** or **HIGH** severity vulnerabilities exist.
3. Ignore unfixed vulnerabilities that have no vendor patch available.
4. Output a summary table to stdout and a detailed SARIF report for security auditing.

??? tip "Reveal Solution"
    ```bash
    #!/usr/bin/env bash
    set -euo pipefail

    IMAGE_NAME="registry.internal/payment-service:1.0.0"

    echo "Running Trivy vulnerability scan against ${IMAGE_NAME}..."

    # Install Trivy if not present:
    # brew install aquasecurity/trivy/trivy OR curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh

    trivy image \
      --severity HIGH,CRITICAL \
      --exit-code 1 \
      --ignore-unfixed \
      --format table \
      "${IMAGE_NAME}"

    echo "Vulnerability scan passed: zero unmitigated HIGH or CRITICAL CVEs detected."
    ```
