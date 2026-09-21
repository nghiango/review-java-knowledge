# Module 24 — Docker

This is a **doc module** covering containerization engineering for Java and Spring Boot backend services:
Docker image architecture, storage drivers and copy-on-write filesystem layers, efficient multi-stage builds, JVM container awareness (cgroups v1/v2, memory limits, CPU quotas, thread pool ergonomics), runtime configuration and secrets management, Docker health checks (`HEALTHCHECK`) and Kubernetes probes alignment, graceful shutdown signal propagation (`SIGTERM` vs `SIGKILL`, PID 1 init process vs dumb-init / tini), non-root security boundaries (`USER`), container vulnerability scanning (Trivy, Grype), and Spring Boot layered JARs (`layertools`) vs Cloud Native Buildpacks (Paketo).

The canonical prose, architecture blueprints, interview Q&A, and operational incident guides live in the documentation:

👉 **[Docker Documentation](../../docs/topics/docker/index.md)**

## Broken Review Examples

This module provides 4 realistic broken review targets under `broken-examples/`:

1. `fat-image-root-user/` — Single-stage Dockerfile packaging a full JDK and build toolchain, running as `root` (UID 0), leading to bloated 1.2GB image size, wide CVE attack surface, and container breakout hazards.
2. `no-memory-limits-jvm/` — Docker Compose deployment and startup script omitting container memory limits and JVM ergonomic flags (`-XX:MaxRAMPercentage`), causing JVM heap calculation against host RAM and sudden Linux kernel Out-Of-Memory (OOM) killer terminations (`Exit Code 137`).
3. `secrets-baked-into-image/` — Dockerfile using `ARG` and `ENV` to inject database passwords and API tokens during build time, permanently baking plaintext credentials into intermediate image layer metadata visible via `docker history` and registry inspection.
4. `missing-healthcheck-shutdown/` — Dockerfile invoking `ENTRYPOINT java -jar app.jar` via shell wrapping (running as non-PID-1 child process), blocking `SIGTERM` signal propagation, preventing Spring graceful shutdown, and lacking container healthcheck definitions.
