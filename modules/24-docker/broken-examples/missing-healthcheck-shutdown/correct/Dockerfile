FROM eclipse-temurin:21-jre-alpine

# Install dumb-init or tini for signal handling and zombie reaping (if running multi-process/scripts)
# or run Java as PID 1 directly using exec array form.
# Alpine includes wget by default for lightweight health probes.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --chown=appuser:appgroup app.jar .

USER appuser:appgroup

EXPOSE 8080

# Configure container healthcheck aligning with Spring Boot Actuator
HEALTHCHECK --interval=10s --timeout=3s --start-period=25s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

# Exec form: Java executes as PID 1, receiving SIGTERM directly from Docker/Kubernetes
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
