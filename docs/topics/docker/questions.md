# Docker Interview Questions

Core interview questions covering Docker image architecture, multi-stage builds, Spring Boot layered JARs, JVM container ergonomics, cgroups v1/v2, non-root security, signal propagation, and production diagnostics.

<!-- --8<-- [start:basic] -->
## Basic

### What is the difference between a Docker image and a Docker container?

??? question "Reveal answer"
    **Short Answer:** A **Docker image** is an immutable, read-only template composed of stacked filesystem layers (libraries, dependencies, binaries) and configuration metadata (entrypoint, environment variables). A **Docker container** is a runnable, isolated instance of an image executed as a Linux process, isolated by kernel namespaces and constrained by cgroups, with a thin writable copy-on-write layer added on top.

    ??? example "Example"
        ```bash
        # Image (read-only template stored on disk / registry)
        docker pull eclipse-temurin:21-jre-alpine

        # Container (active isolated process with writable layer)
        docker run -d --name payment-service -p 8080:8080 eclipse-temurin:21-jre-alpine
        ```

### How do Docker image layers work, and what is the Copy-on-Write (CoW) strategy?

??? question "Reveal answer"
    **Short Answer:** Every instruction in a Dockerfile (`FROM`, `COPY`, `RUN`) creates an immutable, read-only filesystem layer stored in the host storage driver (`overlay2`). Layers are cached by Docker and shared across containers built from common bases. When a container runs, Docker places a thin, mutable **container layer** on top. Under **Copy-on-Write (CoW)**, if a process modifies a file belonging to a lower read-only layer, the storage driver copies the file up into the writable container layer before modifying it, leaving the original layer untouched.

    ??? example "Example"
        ```mermaid
        flowchart TD
            ContainerLayer["Writable Container Layer (modified files stored here)"]
            AppLayer["Read-Only: Application Code Layer"]
            DepsLayer["Read-Only: Third-Party Dependencies Layer"]
            BaseLayer["Read-Only: Base JRE Alpine Layer"]
            
            ContainerLayer --> AppLayer --> DepsLayer --> BaseLayer
        ```

### Why should you use multi-stage builds when containerizing Java applications?

??? question "Reveal answer"
    **Short Answer:** Multi-stage builds separate the build environment from the runtime environment. A heavy build stage (containing the full JDK, Gradle/Maven wrappers, compilers, source code, and cached dependencies, often $>1\text{GB}$) compiles the code, while a second minimal stage (containing only a lightweight JRE or distroless image, $<200\text{MB}$) copies exclusively the final executable JAR. This reduces image size by 70–80%, speeds up deployment push/pull times, and drastically shrinks the attack surface by excluding build tools and compilers from production.

    ??? example "Example"
        ```dockerfile
        # Build stage
        FROM eclipse-temurin:21-jdk-alpine AS builder
        WORKDIR /build
        COPY . .
        RUN ./gradlew bootJar -x test

        # Production runtime stage
        FROM eclipse-temurin:21-jre-alpine
        WORKDIR /app
        COPY --from=builder /build/build/libs/*.jar app.jar
        ENTRYPOINT ["java", "-jar", "app.jar"]
        ```

### What is the difference between `CMD` and `ENTRYPOINT` in a Dockerfile?

??? question "Reveal answer"
    **Short Answer:** `ENTRYPOINT` defines the core executable and fixed parameters that will always run when the container starts. `CMD` provides default arguments to the `ENTRYPOINT` that can be completely overridden by CLI arguments passed to `docker run`. When used together, `ENTRYPOINT` is the fixed binary and `CMD` supplies default parameters.

    ??? example "Example"
        ```dockerfile
        ENTRYPOINT ["java", "-jar"]
        CMD ["app.jar"]
        
        # Running: docker run my-image overrides CMD to "other.jar"
        # Executed command: java -jar other.jar
        ```

### Why should containers run as a non-root user?

??? question "Reveal answer"
    **Short Answer:** By default, containers execute as `root` (UID 0). If an application suffers a Remote Code Execution (RCE) vulnerability (e.g. Log4Shell), the attacker gains root privileges within the container namespace. An attacker with root can inspect mounted secrets, write to system binaries, and exploit Linux kernel or container runtime vulnerabilities (e.g. container breakout bugs) to compromise the underlying host operating system. Creating and switching to an unprivileged user (`USER appuser`) enforces the Principle of Least Privilege.

    ??? example "Example"
        ```dockerfile
        RUN addgroup -S appgroup && adduser -S appuser -G appgroup
        USER appuser:appgroup
        ```

### What is a `.dockerignore` file, and why is it essential for Java projects?

??? question "Reveal answer"
    **Short Answer:** A `.dockerignore` file specifies files and directories that Docker CLI excludes when generating the build context sent to the Docker daemon. In Java projects, excluding `.git`, `build/`, `target/`, `.gradle/`, and local IDE directories (`.idea/`) prevents bloated build contexts (which can be hundreds of megabytes), speeds up build initialization, ensures reproducible builds by not copying local developer classes, and prevents accidental credential leakage (e.g. local `.env` files).

    ??? example "Example"
        ```text
        # .dockerignore
        .git
        .gradle
        build/
        target/
        *.log
        .env
        ```

### What is the difference between Docker `EXPOSE` and publishing ports with `-p`?

??? question "Reveal answer"
    **Short Answer:** `EXPOSE` is merely documentation and image metadata declaring which ports the container service intends to listen on; it does **not** actually open ports on the host. To make a container port accessible to the external network or host machine, you must publish it using the `-p <host_port>:<container_port>` (or `-P` for random host ports) flag at runtime, which instructs Docker to create host `iptables` / NAT routing rules.

    ??? example "Example"
        ```dockerfile
        # Documentation only
        EXPOSE 8080
        ```
        ```bash
        # Actual host network port mapping
        docker run -p 8080:8080 my-spring-app
        ```

### How does Spring Boot's layered JAR feature improve Docker build caching?

??? question "Reveal answer"
    **Short Answer:** Standard Spring Boot fat JARs package dependencies, loader classes, and application code into one monolithic archive. A single code edit alters the entire JAR, forcing Docker to rebuild and push the entire 80MB layer. Spring Boot layered JARs separate the archive into four logical layers: `dependencies`, `spring-boot-loader`, `snapshot-dependencies`, and `application`. In the Dockerfile, layers are copied in order of modification frequency. On routine code changes, only the tiny `application` layer (~2MB) is invalidated and re-uploaded, saving bandwidth and slashing deployment times.

    ??? example "Example"
        ```dockerfile
        # Extract layers
        RUN java -Djarmode=layertools -jar app.jar extract
        
        # Copy in ascending order of change frequency
        COPY --from=builder /build/dependencies/ ./
        COPY --from=builder /build/spring-boot-loader/ ./
        COPY --from=builder /build/snapshot-dependencies/ ./
        COPY --from=builder /build/application/ ./
        ```
<!-- --8<-- [end:basic] -->

<!-- --8<-- [start:intermediate] -->
## Intermediate

### What is the difference between Exec form and Shell form for `ENTRYPOINT` and `CMD`?

??? question "Reveal answer"
    **Short Answer:** **Exec form** (`ENTRYPOINT ["java", "-jar", "app.jar"]`) parses commands as a JSON array and invokes the binary directly without a shell wrapper, making Java **PID 1**. **Shell form** (`ENTRYPOINT java -jar app.jar`) executes the command as `/bin/sh -c "java -jar app.jar"`, making `/bin/sh` PID 1. Because minimal shells do not forward POSIX signals, `SIGTERM` signals sent during container shutdown are swallowed by `/bin/sh`. The JVM never receives `SIGTERM`, preventing Spring graceful shutdown and causing abrupt `SIGKILL` terminations after the timeout window.

    ??? example "Example"
        ```mermaid
        flowchart LR
            Exec["Exec Form: ENTRYPOINT [java, -jar, app.jar]"] --> P1["Java is PID 1 -> Receives SIGTERM directly -> Clean exit"]
            Shell["Shell Form: ENTRYPOINT java -jar app.jar"] --> P2["/bin/sh is PID 1 -> Swallows SIGTERM -> SIGKILL after 10s"]
        ```

### How does the JVM determine its maximum heap size inside a Docker container?

??? question "Reveal answer"
    **Short Answer:** Modern HotSpot JVMs (Java 17 and Java 21) detect container control groups (cgroups v1 and v2) via `osContainer_linux.cpp`. If a container memory limit is set (e.g. `memory.max`), the JVM treats that cgroup limit as its total physical memory. By default, the JVM sets max heap to **25%** of available memory (`-XX:MaxRAMPercentage=25.0`). In production, this should be explicitly set to `-XX:MaxRAMPercentage=75.0` (or `70.0`) so the JVM utilizes 70–75% of the container allocation for heap while reserving 25–30% for native memory (Metaspace, thread stacks, direct byte buffers, GC data structures).

    ??? example "Example"
        ```bash
        # Sizing JVM for a 2GB container:
        # Max Heap = 2048MB * 75% = 1536MB
        # Native Reserve = 512MB
        java -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -jar app.jar
        ```

### What causes a container to exit with Exit Code 137, and how do you diagnose it?

??? question "Reveal answer"
    **Short Answer:** **Exit Code 137** indicates that the process was killed by Linux Signal 9 (`SIGKILL`, where $128 + 9 = 137$). This almost always indicates that the container exceeded its cgroup memory limit (`memory.max`), triggering the Linux kernel **Out-Of-Memory (OOM) Killer**. Because `SIGKILL` is handled by the kernel, no Java `OutOfMemoryError` is thrown, no shutdown hooks run, and no heap dump is created. Diagnose it via `docker inspect <id> --format '{{.State.OOMKilled}}'` (returns `true`) and by inspecting host kernel ring buffers via `dmesg -T | grep -i oom`.

    ??? example "Example"
        ```bash
        # Diagnosing OOM Kill
        docker inspect payment-service --format 'OOMKilled: {{.State.OOMKilled}}, ExitCode: {{.State.ExitCode}}'
        # Output: OOMKilled: true, ExitCode: 137
        
        dmesg -T | grep -E "Out of memory|killed process"
        ```

### What is the role of `tini` or `dumb-init` in Docker containers?

??? question "Reveal answer"
    **Short Answer:** In Unix systems, PID 1 has two mandatory roles: reaping zombie (defunct) child processes and routing POSIX signals. While Java running in exec form handles `SIGTERM` well, if a container runs multiple processes, executes helper scripts, or forks subprocesses (e.g. database tools, agent sidecars), dead child processes accumulate as zombies if Java does not explicitly reap them. Lightweight init systems like `tini` run as PID 1, adopt and reap all zombie processes, and forward `SIGTERM`/`SIGINT` signals reliably to all child processes.

    ??? example "Example"
        ```dockerfile
        RUN apk add --no-cache tini
        ENTRYPOINT ["/sbin/tini", "--", "java", "-jar", "app.jar"]
        ```

### How should build-time secrets be handled in Docker without leaking them into image layers?

??? question "Reveal answer"
    **Short Answer:** Never use `ARG` or `ENV` for sensitive credentials (e.g. GitHub tokens, private Maven repository passwords), as they are permanently baked into image metadata and visible via `docker history <image>`. Instead, use **Docker BuildKit secret mounts** (`--mount=type=secret`). BuildKit mounts the secret into a temporary in-memory filesystem during the `RUN` command; the secret is never committed to any image layer or cache.

    ??? example "Example"
        ```dockerfile
        # syntax=docker/dockerfile:1.4
        FROM eclipse-temurin:21-jdk-alpine AS builder
        WORKDIR /build
        COPY . .
        # Mount secret securely during build
        RUN --mount=type=secret,id=mvn_settings \
            ./gradlew build --no-daemon -x test
        ```
        ```bash
        # Build command passing secret safely:
        DOCKER_BUILDKIT=1 docker build --secret id=mvn_settings,src=~/.m2/settings.xml .
        ```

### What is the purpose of the Docker `HEALTHCHECK` instruction, and how should it align with Spring Boot?

??? question "Reveal answer"
    **Short Answer:** The `HEALTHCHECK` instruction tells the Docker daemon how to test whether the containerized application is healthy and ready to serve traffic. When configured, Docker tracks container state as `starting`, `healthy`, or `unhealthy`. In Docker Compose or swarm, unhealthy containers can be automatically restarted or removed from routing. For Spring Boot services, align `HEALTHCHECK` with Spring Boot Actuator's liveness endpoint:
    `HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=3 CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1`.

    ??? example "Example"
        ```dockerfile
        HEALTHCHECK --interval=10s --timeout=3s --start-period=25s --retries=3 \
          CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1
        ```

### What are the differences between Alpine, Debian Slim, and Distroless base images?

??? question "Reveal answer"
    **Short Answer:**
    - **Alpine** (`eclipse-temurin:21-jre-alpine`): Smallest size (~100MB), uses `musl libc` instead of `glibc`. Highly optimized, but can experience compatibility or performance discrepancies with native libraries (e.g. Netty transport, RocksDB, profilers).
    - **Debian Slim** (`eclipse-temurin:21-jre-jammy`): Standard `glibc`, excellent compatibility, slightly larger size (~200MB), includes standard package manager (`apt-get`).
    - **Google Distroless** (`gcr.io/distroless/java21`): Contains only the application and its runtime dependencies (no package managers, no `/bin/sh`, no shells). Offers the highest security posture because an attacker with RCE has no shell or utilities to execute.

    ??? example "Example"
        ```dockerfile
        # Distroless Java 21 image (no shell, zero package manager attack surface)
        FROM gcr.io/distroless/java21-debian12
        COPY app.jar /app/app.jar
        CMD ["/app/app.jar"]
        ```

### How do CPU limits and CPU quotas affect JVM garbage collection and thread pools?

??? question "Reveal answer"
    **Short Answer:** In Linux cgroups, CPU limits are enforced via Completely Fair Scheduler (CFS) quotas (`cpu.cfs_quota_us` and `cpu.cfs_period_us`). If a container is limited to `cpus: 0.5` or `1.0`, the JVM reads this quota and sets `Runtime.getRuntime().availableProcessors()` accordingly. This directly affects default JVM thread pool sizing: Garbage Collector parallel worker threads, `ForkJoinPool.commonPool()` parallelism, and Netty event loop threads. Setting fractional CPU limits ($< 1.0$) can cause severe CFS quota throttling and high GC pause times. Production backend containers should ideally be allocated $\ge 2.0$ CPU cores to support concurrent GC background threads.

    ??? example "Example"
        ```yaml
        deploy:
          resources:
            limits:
              cpus: "2.0" # Allows JVM to size 2 GC threads & 2 event loop cores
              memory: 4096M
        ```

### How do Docker multi-stage builds and BuildKit cache mounts (`--mount=type=cache`) accelerate Gradle/Maven CI builds?

??? question "Reveal answer"
    **Short Answer:** Standard container builds invalidate local package manager caches on any dependency change, forcing redundant redownloading of hundreds of megabytes of JARs. Docker BuildKit cache mounts (`RUN --mount=type=cache,target=/root/.gradle`) persist the dependency cache directory across image builds on the host engine without baking intermediate cache bloat into the final image layer.

    **Internal Mechanism:** BuildKit mounts an isolated host cache folder onto `/root/.gradle` or `/root/.m2` during the execution of the `RUN` command. In a multi-stage Dockerfile, the builder stage compiles the artifact using the persistent cache, and the final stage (`FROM eclipse-temurin:21-jre`) copies only the compiled runtime JAR. The cached packages never enter the container filesystem layer, producing an ultra-fast build and a lean runtime image.

    **Common Mistake:** Omitting BuildKit cache mounts and instead running `COPY . . && ./gradlew build`, which blows away the local Gradle cache on every single line of code change.

    ??? example "Example"
        ```dockerfile
        # syntax=docker/dockerfile:1
        FROM gradle:8-jdk21 AS builder
        WORKDIR /app
        COPY . .
        # Mount host cache to reuse downloaded artifacts across builds
        RUN --mount=type=cache,target=/root/.gradle \
            gradle build --no-daemon -x test

        FROM eclipse-temurin:21-jre-alpine
        WORKDIR /app
        COPY --from=builder /app/build/libs/app.jar app.jar
        ENTRYPOINT ["java", "-jar", "app.jar"]
        ```

### How do Docker container health checks (`HEALTHCHECK`) interact with orchestrator startup and liveness probes?

??? question "Reveal answer"
    **Short Answer:** Docker `HEALTHCHECK` executes a command periodically inside the container (e.g. `CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1`). Docker sets the container status to `starting`, `healthy`, or `unhealthy`. However, standalone Docker engine does *not* automatically restart unhealthy containers (it only updates the inspection status). In Kubernetes, Dockerfile `HEALTHCHECK` instructions are ignored; Kubernetes uses its own `startupProbe`, `livenessProbe`, and `readinessProbe` to control pod traffic routing and container restarts.

    **Internal Mechanism:** When `HEALTHCHECK` fails consecutively beyond `--retries`, Docker updates `.State.Health.Status` to `unhealthy`. In Docker Compose, services can declare `depends_on: { service: { condition: service_healthy } }` to sequence initialization order. In Kubernetes, the kubelet directly polls HTTP/TCP sockets from outside the container without spawning an exec subshell.

    **Common Mistake:** Relying on Dockerfile `HEALTHCHECK` in Kubernetes clusters, or executing a heavy curl/wget shell every 5 seconds that creates high CPU churn inside a tiny JVM container.

    ??? example "Example"
        ```dockerfile
        # Container-native Docker healthcheck
        HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=3 \
          CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1
        ```
<!-- --8<-- [end:intermediate] -->

<!-- --8<-- [start:senior] -->
## Senior

### How does Java 21 interact with cgroups v1 versus cgroups v2 in modern Linux container runtimes?

??? question "Reveal answer"
    **Short Answer:** In cgroups v1, controllers (memory, CPU, blkio) operate in separate filesystem trees (`/sys/fs/cgroup/memory/...`), causing synchronization bugs and poor swap handling. In cgroups v2, a single unified hierarchy (`/sys/fs/cgroup/memory.max`, `/sys/fs/cgroup/cpu.max`) provides consolidated resource accounting and Pressure Stall Information (PSI). Modern Java 21 fully supports both: HotSpot inspects `/proc/self/mountinfo` and `/proc/self/cgroup` to detect cgroup version. Under cgroups v2, Java 21 calculates heap and processor availability more accurately and can report cgroup metrics via Java Management Extensions (JMX) and `OperatingSystemMXBean`.

    ??? example "Example"
        ```bash
        # Verifying cgroup version inside container:
        stat -fc %T /sys/fs/cgroup/
        # Output: cgroup2fs (cgroups v2) or tmpfs (cgroups v1)
        ```

### What is the "Native Memory Drift" problem in containerized Java, and how do you size container RAM limits to prevent it?

??? question "Reveal answer"
    **Short Answer:** Many teams assume setting `-Xmx4g` inside a 4.5GB container is safe. However, the JVM allocates substantial off-heap native memory outside the heap: Metaspace (dynamic class generation, Spring proxies), platform thread stacks (1MB per thread, so 200 threads = 200MB), direct byte buffers (Netty socket channels), JIT code cache (240MB), and GC management tables. Under high network traffic, Netty direct memory buffers expand. Once $\text{Heap} + \text{Native} > 4.5\text{GB}$, the Linux kernel executes `SIGKILL` (Exit Code 137). Sizing rules:
    1. Set `-XX:MaxRAMPercentage=70.0` to guarantee a 30% buffer for native allocations.
    2. Monitor native memory using Native Memory Tracking (`-XX:NativeMemoryTracking=summary` and `jcmd <pid> VM.native_memory`).
    3. Cap Metaspace with `-XX:MaxMetaspaceSize=384m`.

    ??? example "Example"
        ```bash
        # JVM arguments for container memory safety:
        -XX:MaxRAMPercentage=70.0 \
        -XX:MaxMetaspaceSize=384m \
        -XX:+ExitOnOutOfMemoryError
        ```

### How do you implement zero-downtime rolling deployments combining Spring Boot graceful shutdown and Docker/Kubernetes signal windows?

??? question "Reveal answer"
    **Short Answer:** Zero-downtime rolling deploys require coordinating the orchestrator's traffic deregistration with Spring Boot's connection draining:
    1. **Spring Configuration**: Enable `server.shutdown: graceful` with a drain timeout (e.g. `spring.lifecycle.timeout-per-shutdown-phase: 20s`).
    2. **Signal Delivery**: Ensure Java runs as PID 1 via exec form `ENTRYPOINT ["java", "-jar", "app.jar"]` so it receives `SIGTERM` instantly.
    3. **Pre-Stop Delay (Kubernetes / Gateway)**: When a pod is deleted, iptables/endpoint propagation takes 2–5 seconds. If the container stops immediately, in-flight requests in the network pipeline hit closed sockets. A `preStop: sleep 5` hook delays container termination until the proxy removes the pod from the routing table.
    4. **Grace Period Alignment**: Orchestrator termination grace period must exceed the sum of pre-stop delay and Spring drain timeout: $\text{TerminationGracePeriodSeconds} (35\text{s}) > \text{preStop} (5\text{s}) + \text{SpringDrain} (20\text{s})$.

    ??? example "Example"
        ```yaml
        # Kubernetes Zero-Downtime Pod Lifecycle Alignment
        lifecycle:
          preStop:
            exec:
              command: ["/bin/sh", "-c", "sleep 5"]
        terminationGracePeriodSeconds: 35
        ```
        ```yaml
        # application.yml
        server:
          shutdown: graceful
        spring:
          lifecycle:
            timeout-per-shutdown-phase: 20s
        ```

### Why should you prefer Cloud Native Buildpacks (Paketo) over custom Dockerfiles for enterprise Spring Boot services?

??? question "Reveal answer"
    **Short Answer:** While custom Dockerfiles offer flexibility, they create massive maintenance liabilities across hundreds of enterprise microservices: teams copy-paste insecure base images, run as root, misconfigure JVM ergonomics, and fail to patch operating system CVEs. **Cloud Native Buildpacks (CNB)** (supported natively in Spring Boot via `./gradlew bootBuildImage` or `pack` CLI):
    1. Automatically detect Java version and apply optimal layered JAR extraction.
    2. Calculate memory ergonomics dynamically at container launch using the memory calculator.
    3. Automatically enforce non-root unprivileged users.
    4. Support **Image Re-basing**: OS-level CVE patches (e.g. OpenSSL vulnerabilities in base layers) can be applied to thousands of images in seconds without recompiling application code or rebuilding layers.

    ??? example "Example"
        ```bash
        # Generate production-ready, secure, layered OCI container image without Dockerfile:
        ./gradlew bootBuildImage --imageName=registry.internal/payment-service:1.0.0
        ```

### How do you diagnose and triage a memory leak in a containerized Java service when `jcmd` and `jmap` fail due to container isolation?

??? question "Reveal answer"
    **Short Answer:** In hardened production containers (non-root user, read-only root filesystems, distroless images), running `jcmd` or `jmap` directly fails because debug tools and shells are absent, and process ownership permissions prevent foreign attachments.
    1. **Ephemeral Debug Containers (Kubernetes)**: Attach a debug container using `kubectl debug -it <pod> --image=eclipse-temurin:21-jdk --target=<container-name>`, sharing the target process namespace so `jcmd` can attach.
    2. **Docker Process Sharing**: Use `docker run --rm -it --pid=container:<target-id> --cap-add=SYS_PTRACE eclipse-temurin:21-jdk jcmd 1 GC.heap_dump /tmp/heap.hprof`.
    3. **Automated OOM Dumps**: Configure JVM flags to dump heap before exit: `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps/heap.hprof`, mounting `/dumps` to an external host volume or persistent volume claim.

    ??? example "Example"
        ```bash
        # Attaching to running container process namespace to capture heap dump
        docker run --rm -it \
          --pid=container:order-service-app \
          --cap-add=SYS_PTRACE \
          -v /tmp/dumps:/dumps \
          eclipse-temurin:21-jdk \
          jcmd 1 GC.heap_dump /dumps/heap.hprof
        ```

### How does Rootless Docker daemon architecture and user namespace remapping (`userns-remap`) prevent privilege escalation attacks?

??? question "Reveal answer"
    **Short Answer:** In traditional Docker, the daemon runs as `root` on the host, meaning container processes running as UID 0 have real `root` capabilities if a container breakout vulnerability occurs. User namespace remapping (`userns-remap`) maps the container's internal UID 0 (root) to an unprivileged sub-UID on the host (e.g. UID 100000). Rootless Docker runs the entire daemon and container runtime inside an unprivileged user namespace, completely preventing host-level root compromise.

    **Internal Mechanism:** The Linux kernel `user_namespaces(7)` facility isolates security-related identifiers (UIDs, GIDs, capabilities, keys). When a container root process attempts to access a host file, the host kernel checks permissions against the mapped unprivileged host UID (e.g. 100000). Even with `CAP_SYS_ADMIN` inside the container user namespace, the process cannot modify host system devices or load kernel modules.

    **Common Mistake:** Running containers with `--privileged` in an attempt to bypass permissions issues, completely disabling user namespaces, AppArmor, seccomp, and cgroup restrictions.

    ??? example "Example"
        ```json
        // /etc/docker/daemon.json
        {
          "userns-remap": "default"
        }
        ```

### How do Linux cgroups v2 memory controllers (`memory.max` vs `memory.high`) manage container JVM heap pressure and kernel page caching?

??? question "Reveal answer"
    **Short Answer:** In Linux cgroups v2, memory limits are structured with two distinct thresholds:
    1. **`memory.max`**: Hard limit. Exceeding this limit immediately triggers the Linux kernel Out-Of-Memory (OOM) Killer, terminating the process with `SIGKILL` (Exit 137).
    2. **`memory.high`**: Throttle/reclaim threshold. When memory usage exceeds `memory.high`, the kernel throttles allocations and aggressively reclaims file caches without invoking the OOM Killer.
    In Java 21+, JVM container awareness reads cgroups v2 files directly (`/sys/fs/cgroup/memory.max`), correctly calculating heap proportions without falling back to host memory values.

    **Internal Mechanism:** When usage passes `memory.high`, the kernel forces the allocating threads into direct reclaim loops. If the container process is unable to free memory (e.g. JVM heap is filled with live objects), memory climbs toward `memory.max`. At `memory.max`, the kernel selects a victim process using `oom_score` and sends `SIGKILL`.

    **Common Mistake:** Confusing container memory with JVM heap size. Setting `memory.max=2GB` and `-Xmx2GB` leaves 0MB for JVM off-heap native memory (Metaspace, thread stacks, direct byte buffers, JIT code cache), guaranteeing kernel OOM kills under load.

    ??? example "Example"
        ```bash
        # Inspecting cgroups v2 limits inside modern container:
        cat /sys/fs/cgroup/memory.max
        cat /sys/fs/cgroup/memory.high
        cat /sys/fs/cgroup/memory.current
        ```

### How do container network namespaces (bridge vs host networking) impact JVM network throughput and socket latency?

??? question "Reveal answer"
    **Short Answer:** Docker default **bridge** networking places each container in an isolated network namespace connected to `docker0` via a `veth` pair, routing inbound/outbound packets through host `iptables` NAT (Network Address Translation). For ultra-high-throughput JVM microservices (>50,000 req/sec), bridge NAT traversal adds packet latency, connection tracking (`conntrack`) table exhaustion, and CPU overhead. **Host** networking (`--network host`) binds the container directly to the host's network stack, eliminating packet transformation and achieving bare-metal network performance.

    **Internal Mechanism:** With bridge networking, every packet traverses Linux netfilter rules (`PREROUTING`, `POSTROUTING`). Under high connection churn (e.g. non-keepalive HTTP or Redis queries), the Linux kernel `nf_conntrack` table fills up, dropping incoming TCP SYN packets with `table full, dropping packet`. Host networking bypasses netfilter NAT completely.

    **Common Mistake:** Using `--network host` without port isolation, causing port collisions between multiple container replicas running on the same host node.

    ??? example "Example"
        ```yaml
        services:
          order-service:
            image: order-service:latest
            # High-throughput bare-metal network performance
            network_mode: host
        ```
<!-- --8<-- [end:senior] -->

<!-- --8<-- [start:scenarios] -->
### Incident: Hardcoded JVM heap size inside a container triggers mysterious Exit Code 137 under peak traffic. Diagnose and resolve.

??? question "Reveal answer"
    **Short Answer:** A Spring Boot container allocated with a 2048MB cgroup limit ran with `java -Xmx1800m -jar app.jar`. During flash sale traffic, concurrent HTTP connections increased Netty direct memory buffers, Metaspace reached 150MB, and thread stacks consumed 100MB. Total process Resident Set Size (RSS) reached 2150MB, exceeding the 2048MB container limit. The Linux kernel Out-Of-Memory (OOM) Killer immediately terminated the container with `SIGKILL` (Signal 9, $128 + 9 = 137$), generating no Java `OutOfMemoryError` or logs.

    **Remediation:**
    1. **Adopt Container Ergonomics**: Replace static `-Xmx` with `-XX:MaxRAMPercentage=70.0 -XX:MaxMetaspaceSize=256m` so heap automatically scales with container limits while reserving 30% for native allocations.
    2. **Container Headroom**: Increase container memory ceiling to 3072M if the workload demands 1.8GB of heap.
    3. **Telemetry Alerting**: Monitor Prometheus metric `container_memory_working_set_bytes / container_spec_memory_limit_bytes > 0.85`.

    ??? example "Example"
        ```bash
        # Diagnosing cgroup OOM termination:
        docker inspect order-service --format 'OOM: {{.State.OOMKilled}}, Exit: {{.State.ExitCode}}'
        # Output: OOM: true, Exit: 137

        # Checking host kernel ring buffer:
        dmesg -T | grep -i "killed process"
        ```

### Incident: Shell form entrypoint swallows SIGTERM, causing dropped transactions and 10-second deployment hangs. Diagnose and resolve.

??? question "Reveal answer"
    **Short Answer:** The container entrypoint was written in shell form: `ENTRYPOINT java -jar app.jar`. Inside the container, `/bin/sh` ran as PID 1 and Java ran as child PID 7. Minimal Linux shells (`ash`/`sh`) do not forward POSIX signals to child processes. When Kubernetes or Docker sent `SIGTERM`, `/bin/sh` swallowed the signal. The JVM never received notification to start Spring Boot graceful shutdown (`server.shutdown: graceful`). After the 10–30 second termination grace period expired, the orchestrator forcefully sent `SIGKILL`, severing in-flight payment transactions mid-authorization.

    **Remediation:**
    1. **Switch to Exec Form**: Use JSON array syntax `ENTRYPOINT ["java", "-jar", "app.jar"]` so Java runs as PID 1 and receives `SIGTERM` instantly.
    2. **Signal Forwarding Daemon**: Alternatively use `tini` or `dumb-init` as PID 1 to guarantee signal propagation and zombie child reaping.
    3. **Pre-Stop Delay Alignment**: In Kubernetes, add a 5-second `preStop` delay (`sleep 5`) to allow ingress routing tables to remove the pod IP before Spring starts draining connections.

    ??? example "Example"
        ```dockerfile
        # Fixed Dockerfile using JSON array exec form:
        ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
        ```

### Incident: Container JVM OOMKilled by Linux kernel (Exit Code 137) due to disabled container support (`-XX:-UseContainerSupport`). Diagnose and resolve.

??? question "Reveal answer"
    **Short Answer:** In legacy deployments or images with misconfigured JVM flags, explicit flag `-XX:-UseContainerSupport` disabled HotSpot's container detection heuristics. The JVM detected the entire host physical RAM (e.g. 64GB) instead of the pod's 2GB cgroup limit. By default, HotSpot allocated 25% of 64GB = 16GB for heap. As soon as heap allocations climbed past 2GB, the Linux kernel cgroup controller immediately invoked the OOM Killer, terminating the JVM with `ExitCode 137`.

    **Remediation:**
    1. **Verify Container Support**: Ensure `-XX:+UseContainerSupport` is enabled (default in modern JDK 17/21).
    2. **Percentage-Based Heap**: Set `-XX:InitialRAMPercentage=50.0 -XX:MaxRAMPercentage=75.0`.
    3. **Diagnose via Actuator/JVM**: Check `Runtime.getRuntime().maxMemory()` in `/actuator/env` to verify the JVM reads the container limit rather than host RAM.

    ??? example "Example"
        ```bash
        # Verify JVM container detection and calculated limits:
        java -XX:+PrintFlagsFinal -version | grep -i UseContainerSupport
        # Output: bool UseContainerSupport = true

        # Checking effective max memory under 2G container limit:
        docker run --rm -m 2g eclipse-temurin:21-jre java -XshowSettings:system -version
        ```

### Incident: Docker image build failed or slowed to a crawl in CI due to layer cache invalidation caused by unpinned timestamps and misplaced `apt-get` instructions. Diagnose and resolve.

??? question "Reveal answer"
    **Short Answer:** A team added `COPY . .` at line 3 of their Dockerfile, followed by `RUN apt-get update && apt-get install -y curl`. Because source files change on every git commit, the layer cache for `COPY . .` was invalidated every build. Docker was forced to re-run `apt-get update` against remote package mirrors on every commit, multiplying CI build time from 20 seconds to 8 minutes, and frequently failing when external Debian mirrors were throttled or returned 503 errors.

    **Remediation:**
    1. **Reorder Layers by Volatility**: Move OS package installation and tool configuration to the very top of the Dockerfile, *before* copying any project files.
    2. **Clean Package Caches**: Chain `rm -rf /var/lib/apt/lists/*` in the same `RUN` step to keep layer size small.
    3. **Copy Manifests First**: Copy only dependency descriptor files (`build.gradle.kts`, `settings.gradle.kts`) to download dependencies before copying mutable source code.

    ??? example "Example"
        ```dockerfile
        FROM eclipse-temurin:21-jdk-jammy AS builder
        WORKDIR /app

        # 1. Stable layer: OS tools cached indefinitely
        RUN apt-get update && apt-get install -y --no-install-recommends curl \
            && rm -rf /var/lib/apt/lists/*

        # 2. Medium volatility: Gradle wrapper and configs
        COPY gradlew settings.gradle.kts build.gradle.kts ./
        COPY gradle/ gradle/
        RUN ./gradlew dependencies --no-daemon

        # 3. High volatility: Application source code
        COPY src/ src/
        RUN ./gradlew bootJar --no-daemon -x test
        ```
<!-- --8<-- [end:scenarios] -->
