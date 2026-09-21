# Docker Solutions

Walkthrough of production-ready container engineering solutions addressing image bloat, root execution, cgroup memory limits, secret leakage, and graceful shutdown signal handling.

---

## Multi-Stage Builds, Layered JARs, and Non-Root Execution

The corrected Dockerfile implements a two-stage build: an ephemeral build stage that resolves dependencies and extracts the Spring Boot layered JAR, followed by a minimal production runtime stage running as an unprivileged user.

### Corrected Implementation

```dockerfile
--8<-- "modules/24-docker/broken-examples/fat-image-root-user/correct/Dockerfile"
```

### Why It Works

1. **Layer Caching Optimization**:
   - `COPY gradlew`, `gradle/`, `build.gradle.kts`, and `settings.gradle.kts` are copied first, followed by `./gradlew dependencies`.
   - As long as project build manifests do not change, Docker caches all downloaded third-party libraries across builds.
2. **Spring Boot Layered JARs (`layertools`)**:
   - `java -Djarmode=layertools -jar app.jar extract` unpacks the fat JAR into four layers: `dependencies/`, `spring-boot-loader/`, `snapshot-dependencies/`, and `application/`.
   - Layers are copied in ascending order of modification frequency. On routine source edits, only the final `application/` layer (~2MB) is invalidated and re-uploaded.
3. **Minimal Attack Surface**:
   - The runtime stage uses `eclipse-temurin:21-jre-alpine` (~140MB) instead of a full JDK (>600MB), discarding the compiler, headers, and build toolchains.
4. **Hardened Non-Root Security**:
   - `addgroup -S appgroup && adduser -S appuser -G appgroup` creates a dedicated system account.
   - `USER appuser:appgroup` drops root privileges before application startup, preventing container breakout and host compromise upon potential RCE vulnerabilities.

### Trade-offs

- **Alpine `musl` vs `glibc`**: Alpine uses `musl libc`, which occasionally exhibits performance quirks with certain specialized native libraries (e.g. Netty native epoll, async-profiler). If strict `glibc` compatibility is required, swap the base image to `eclipse-temurin:21-jre-jammy` (Ubuntu/Debian slim, ~220MB).

---

## JVM Container Memory Ergonomics and cgroups

The corrected Docker Compose configuration declares explicit container memory limits and configures the JVM to dynamically calculate heap size using container ergonomics flags.

### Corrected Implementation

```yaml
--8<-- "modules/24-docker/broken-examples/no-memory-limits-jvm/correct/docker-compose.yml"
```

### Why It Works

1. **Explicit Cgroup Resource Limits**:
   - `limits.memory: 4096M` sets the Linux cgroup memory ceiling (`memory.max`). The container process group is strictly bounded, preventing host starvation.
   - `reservations.memory: 2048M` guarantees that the host scheduler reserves at least 2GB of physical memory for this service.
2. **Dynamic Container Ergonomics**:
   - `-XX:MaxRAMPercentage=75.0` instructs the HotSpot JVM to size its maximum heap to exactly $75\%$ of the cgroup limit ($4096\text{MB} \times 0.75 = 3072\text{MB}$).
   - The remaining $25\%$ ($1024\text{MB}$) is reserved for off-heap native memory: Metaspace, thread stacks, direct byte buffers (Netty channels), JIT code cache, and garbage collection management tables.
3. **Fail-Fast OOM Safety**:
   - `-XX:+ExitOnOutOfMemoryError` guarantees that if the JVM experiences an internal heap `OutOfMemoryError`, the process terminates immediately rather than lingering in a corrupted, zombie state.

### Trade-offs

- **Percentage Sizing vs Fixed Limits**: For very large containers (e.g. 64GB limit), reserving 25% for native memory leaves 16GB unused, which may be excessive. For containers $\ge 16\text{GB}$, tune `-XX:MaxRAMPercentage=80.0` or `85.0`.

---

## Secure Secrets Management and BuildKit Mounts

The corrected Dockerfile uses Docker BuildKit secret mounts to access private build credentials without persisting them into any filesystem layer or image manifest.

### Corrected Implementation

```dockerfile
--8<-- "modules/24-docker/broken-examples/secrets-baked-into-image/correct/Dockerfile"
```

### Why It Works

1. **Zero Secret Persistence**:
   - `RUN --mount=type=secret,id=github_token ...` mounts the secret file into `/run/secrets/github_token` as a temporary in-memory tmpfs mount during step execution only.
   - The secret file is never written to disk layers and is invisible in `docker history` or image configuration JSON.
2. **Runtime Decoupling**:
   - Application credentials (database passwords, API keys) are not set via `ENV`. They are passed dynamically at runtime via environment variables (`docker run -e DB_PASSWORD=...`), Kubernetes Secrets (`secretKeyRef`), or AWS Secrets Manager.

### Trade-offs

- **BuildKit Prerequisite**: Requires Docker BuildKit (`DOCKER_BUILDKIT=1` or Docker v20.10+), which is the default in modern Docker setups.

---

## Graceful Shutdown Signal Propagation and Healthchecks

The corrected Dockerfile uses the JSON array exec form to run Java as PID 1, adds Spring Boot Actuator health monitoring, and ensures zero-downtime signal handling.

### Corrected Implementation

```dockerfile
--8<-- "modules/24-docker/broken-examples/missing-healthcheck-shutdown/correct/Dockerfile"
```

### Why It Works

1. **Direct Signal Delivery to PID 1**:
   - `ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]` executes Java directly as PID 1 inside the container namespace without a shell wrapper.
   - When Docker or Kubernetes initiates container shutdown, the `SIGTERM` signal is delivered directly to the HotSpot JVM, triggering Spring Boot's graceful shutdown hook.
2. **Spring Boot Connection Draining**:
   - Spring Boot stops accepting new requests, drains active HTTP connections, and gracefully finishes in-flight database transactions before exiting cleanly with `Exit Code 0`.
3. **Actuator Health Alignment**:
   - The `HEALTHCHECK` directive polls `http://localhost:8080/actuator/health/liveness` every 10 seconds.
   - `--start-period=25s` gives Spring Boot adequate time to initialize without triggering false-positive restart loops during startup.

### Trade-offs

- **PID 1 Signal Semantics**: If running complex entrypoint scripts or sidecar binaries within the same container, Java as PID 1 cannot reap zombie processes created by external scripts. In multi-process containers, use `tini` as PID 1 (`ENTRYPOINT ["/sbin/tini", "--", "java", "-jar", "app.jar"]`).
