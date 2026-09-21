# Docker Code Review

Review each clean Dockerfile and container configuration before expanding its answer and architectural explanations.

---

## Fat single-stage image running as root

A development team containerized a Spring Boot 3.5 payment service using Docker. An engineer authored `Dockerfile` to build and package the application artifact.

```dockerfile
--8<-- "modules/24-docker/broken-examples/fat-image-root-user/Dockerfile"
```

Consider base image sizing, build toolchain leakage, attack surface expansion, layer caching order, and root user execution boundaries.

??? warning "Reveal issues"
    **Security issue — container runs as default root user (UID 0):** No unprivileged user is created or declared. In the event of an arbitrary code execution vulnerability (e.g. Log4Shell / RCE), an attacker gains root privileges within the container, enabling local file tampering, privilege escalation, and potential container breakout onto the host system.

    **Performance issue — single-stage build creates bloated fat image leaking build tools and caches:** The runtime image packages the full JDK (`eclipse-temurin:21-jdk`), Gradle caches, source code, and intermediate build artifacts, bloating image size beyond 1.2 GB and dramatically expanding the CVE vulnerability footprint.

    **Performance issue — cache busting due to premature `COPY . .`:** Copying the entire repository before dependency resolution invalidates the Docker build cache on every file change, forcing Gradle to re-download all dependencies on every build.

[Correct implementation](solutions.md#multi-stage-builds-layered-jars-and-non-root-execution)

---

## JVM running in container without memory limits

A high-throughput order service is deployed to production via Docker Compose on a host node with 64 GB of physical RAM. The container configuration and startup script were authored by an infrastructure engineer.

```yaml
--8<-- "modules/24-docker/broken-examples/no-memory-limits-jvm/docker-compose.yml"
```

Consider cgroup memory constraints, static heap flags (`-Xmx32g`), JVM container ergonomics, native off-heap allocations, and Linux Out-Of-Memory (OOM) killer terminations (`Exit Code 137`).

??? warning "Reveal issues"
    **Memory issue — static `-Xmx` misaligned with container cgroup limits triggers OOM Killer (Exit Code 137):** Hardcoding `-Xmx32g` ignores container cgroup boundaries and leaves no headroom for native off-heap memory (Metaspace, thread stacks, direct byte buffers, GC tables). If container memory limits are exceeded, the Linux kernel terminates the container abruptly with `SIGKILL` (`Exit Code 137`) without logging an error.

    **Configuration issue — missing container memory and CPU limits in Docker Compose:** Without explicit container resource limits (`limits.memory`, `limits.cpus`), an unchecked memory leak or traffic burst can consume all 64 GB of host RAM, starving the host kernel and adjacent services.

[Correct implementation](solutions.md#jvm-container-memory-ergonomics-and-cgroups)

---

## Secrets baked into Docker image

A microservice Dockerfile configures database credentials and a private GitHub token to clone private repositories during the Docker build process.

```dockerfile
--8<-- "modules/24-docker/broken-examples/secrets-baked-into-image/Dockerfile"
```

Consider build arguments (`ARG`), environment variables (`ENV`), image layer history inspection (`docker history`), and secure build secret mounting.

??? warning "Reveal issues"
    **Security issue — sensitive credentials baked into `ARG` and `ENV` metadata:** Values passed via `ARG` and set in `ENV` are permanently committed into the immutable image metadata. Anyone with read access to the image registry can extract plaintext passwords and API tokens using `docker history --no-trunc <image>` or `docker inspect <image>`.

    **Observability issue — credential logging in build outputs:** Printing token values or masked prefixes during Docker build commands writes sensitive credentials directly to CI/CD build logs.

[Correct implementation](solutions.md#secure-secrets-management-and-buildkit-mounts)

---

## Shell form entrypoint blocking SIGTERM and missing healthcheck

A production checkout service running on Docker frequently drops in-flight customer transactions during rolling deployments and container redeployments. Orchestration tools report that containers take 10 seconds to terminate before being killed forcefully.

```dockerfile
--8<-- "modules/24-docker/broken-examples/missing-healthcheck-shutdown/Dockerfile"
```

Consider shell form vs exec form entrypoints, PID 1 Unix signal propagation, Spring Boot graceful shutdown (`server.shutdown: graceful`), and container health monitoring.

??? warning "Reveal issues"
    **Reliability issue — shell form ENTRYPOINT breaks `SIGTERM` signal propagation and graceful shutdown:** Using shell form `ENTRYPOINT java -jar app.jar` causes `/bin/sh` to run as PID 1. Minimal shells do not forward POSIX signals to child processes. The JVM never receives `SIGTERM`, preventing Spring graceful shutdown from draining connections until Docker forcefully terminates the process with `SIGKILL` after the 10-second timeout.

    **Reliability issue — missing `HEALTHCHECK` directive allows routing to unready containers:** Without a healthcheck probe, load balancers and orchestrators route traffic to the container before Spring Boot finishes initializing, causing connection errors and 502 Bad Gateway responses.

[Correct implementation](solutions.md#graceful-shutdown-signal-propagation-and-healthchecks)
