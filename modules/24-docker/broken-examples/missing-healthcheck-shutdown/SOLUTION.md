# Solution — Shell Form Entrypoint Blocking SIGTERM and Missing Healthcheck

## Annotated code

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY app.jar .

EXPOSE 8080

# Reliability issue: Missing HEALTHCHECK instruction.
# Without a container healthcheck, Docker and upstream load balancers treat the container as healthy
# as soon as the process starts, routing client traffic before Spring Boot finishes initializing (causing HTTP 502/503s).
# Furthermore, if the JVM suffers deadlock or unresponsive event loops, the container is never marked unhealthy or restarted.

# Reliability issue: Shell form ENTRYPOINT executes /bin/sh -c "java -jar app.jar".
# The shell process becomes PID 1 inside the container namespace. Most minimal shells (/bin/sh, dash, ash)
# DO NOT forward POSIX signals (such as SIGTERM) to child processes.
# When Docker or Kubernetes stops the container (`docker stop`), the SIGTERM signal is delivered to /bin/sh and swallowed.
# The Java application never receives SIGTERM, Spring Boot's graceful shutdown hook is never executed, and in-flight
# transactions are abruptly severed when the orchestrator fires SIGKILL (kill -9) after the 10-second timeout.
ENTRYPOINT java -jar app.jar
```

## Issue list

### Reliability issue: Shell form ENTRYPOINT breaks `SIGTERM` signal propagation and graceful shutdown

- **Location:** `Dockerfile:12`
- **Description:** Using the string form `ENTRYPOINT java -jar app.jar` instead of JSON array exec form `ENTRYPOINT ["java", "-jar", "app.jar"]`.
- **Impact:** The container process tree runs `/bin/sh` as PID 1, which ignores `SIGTERM`. When `docker stop` or Kubernetes pod eviction occurs, the JVM never initiates Spring graceful shutdown (`server.shutdown: graceful`). After 10–30 seconds, Docker issues an uncatchable `SIGKILL`, abruptly aborting active database transactions and customer HTTP connections.
- **Remediation:** Use exec form `ENTRYPOINT ["java", "-jar", "app.jar"]` so Java runs directly as PID 1, or introduce a lightweight init daemon such as `tini` (`dumb-init`) to forward POSIX signals and reap zombie child processes.

### Reliability issue: Missing `HEALTHCHECK` directive allows routing to unready containers

- **Location:** `Dockerfile:8`
- **Description:** No container-level health probe is configured.
- **Impact:** Load balancers start routing live traffic immediately upon container socket binding, before Spring Boot finishes application context startup, resulting in connection refusions and 502 Bad Gateway errors. Stalled containers with deadlocked worker pools are never terminated or restarted.
- **Remediation:** Add `HEALTHCHECK` using `curl` or `wget` targeting Spring Boot Actuator's health endpoint: `HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=3 CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1`.

## Correct implementation

See [`correct/Dockerfile`](correct/Dockerfile).

Detailed discussion in [Solutions](../../../docs/topics/docker/solutions.md#graceful-shutdown-signal-propagation-and-healthchecks).
