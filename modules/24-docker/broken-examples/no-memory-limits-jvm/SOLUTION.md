# Solution — JVM Running in Container Without Memory Limits

## Annotated code

```yaml
services:
  order-service:
    image: order-service:1.0.0
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    # Performance issue: Docker container has no memory limit (mem_limit or cgroup memory constraint).
    # If unconstrained, a container can consume all available host RAM, starving the host operating system
    # and neighboring containers, triggering unpredictable host kernel Out-Of-Memory thrashing.
    # Resource leak issue: Missing cpu limits and reservation guarantees.
    entrypoint: ["/entrypoint.sh"]

# entrypoint.sh:
# #!/bin/sh
# # Memory issue: Static -Xmx32g hardcoding does not adapt to container cgroup limits.
# # If this container is given a 4GB or 8GB cgroup limit in Kubernetes or Compose, the JVM attempts
# # to allocate up to 32GB. As soon as resident memory exceeds the cgroup boundary, the Linux kernel OOM Killer
# # abruptly terminates the JVM process with SIGKILL (Exit Code 137), producing no JVM heap dump or error log.
# # Configuration issue: Ignores JVM container ergonomics (-XX:MaxRAMPercentage) and native memory overhead
# # (Metaspace, GC structures, Thread stacks, DirectByteBuffers).
# java -Xmx32g -jar /app/app.jar
```

## Issue list

### Memory issue: Static `-Xmx` misaligned with container cgroup limits triggers OOM Killer (Exit Code 137)

- **Location:** `entrypoint.sh:4`
- **Description:** Hardcoding a static heap size (`-Xmx32g`) ignores container boundaries. Total JVM process memory consists of: $\text{Total Memory} = \text{Heap} + \text{Metaspace} + \text{Thread Stacks} + \text{Direct Buffers} + \text{Native Code/JVM internals}$.
- **Impact:** If the container memory ceiling is lower than `-Xmx` plus native overhead, the Linux kernel detects a cgroup memory violation and sends an instantaneous `SIGKILL` (`kill -9`, exit code 137). No Java `OutOfMemoryError` is thrown, no shutdown hooks run, and no heap dump (`hprof`) is generated.
- **Remediation:** Remove hardcoded `-Xmx` values and configure container ergonomics: `-XX:MaxRAMPercentage=75.0` (or `70.0`), allowing 25–30% of container memory for off-heap, Metaspace, and OS buffers.

### Configuration issue: Missing container memory and CPU limits in Docker Compose

- **Location:** `docker-compose.yml:2`
- **Description:** No `mem_limit` / `deploy.resources.limits.memory` or CPU limits are defined.
- **Impact:** A memory leak or high-concurrency surge within `order-service` can consume all 64GB of host memory, starving the Docker daemon, host kernel, and adjacent microservices.
- **Remediation:** Explicitly declare resource limits and reservations under `deploy.resources.limits` in Docker Compose (e.g. `memory: 4G`, `cpus: '2.0'`).

## Correct implementation

See [`correct/docker-compose.yml`](correct/docker-compose.yml).

Detailed discussion in [Solutions](../../../docs/topics/docker/solutions.md#jvm-container-memory-ergonomics-and-cgroups).
