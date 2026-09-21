# Docker in Production

Operational failure modes, incident post-mortems, telemetry monitoring, and a production readiness checklist for running containerized Spring Boot applications.

---

## Production Incident 1: The Mysterious Exit Code 137 Under Traffic Surge

### Incident Timeline

| Time | Event |
|---|---|
| **11:00** | Marketing launches flash sale. Request traffic to `order-service` surges from 200 req/sec to 3,500 req/sec. |
| **11:04** | Container CPU utilization spikes to 85%. P99 latency remains stable at 25ms. |
| **11:05** | `order-service` container vanishes instantly without emitting error logs. Kubernetes reports: `OOMKilled: true, ExitCode: 137`. |
| **11:06** | Container restarts automatically. As soon as flash traffic hits the cold JVM, it is killed again with `Exit Code 137` within 45 seconds. Crash loop persists for 15 minutes. |
| **11:20** | SRE team doubles the container memory limit from `2048M` to `4096M`. Service stabilizes immediately. |

### Root Cause Analysis

Inspection of the deployment manifest and JVM parameters revealed:

```yaml
# Pod container resource limits:
resources:
  limits:
    memory: "2048Mi"
```
```bash
# Container JVM startup command:
java -Xmx1800m -jar /app/order-service.jar
```

The developer configured a static `-Xmx1800m` inside a 2048MB container, leaving only 248MB ($2048 - 1800$) for the entire host process.

Total resident memory consumed by the JVM:

$$\text{RSS} = \text{Heap} (1800\text{MB}) + \text{Metaspace} (140\text{MB}) + \text{Thread Stacks} (80\text{MB}) + \text{Direct Buffers} (120\text{MB}) + \text{GC Overhead} (60\text{MB}) = 2200\text{MB}$$

When flash traffic opened 400 concurrent Netty socket channels, direct byte buffer allocations pushed total container memory to 2,200MB, breaching the 2,048MB cgroup limit (`memory.max`). The Linux kernel cgroup OOM Killer immediately dispatched `SIGKILL` (Signal 9, $128 + 9 = 137$) to PID 1.

Because `SIGKILL` cannot be caught or handled by user space, the JVM was obliterated instantly without generating an `OutOfMemoryError` stack trace or heap dump.

### Remediation & Preventive Measures

1. **Configure Container Ergonomics**: Replace static `-Xmx` with dynamic percentage scaling:
   ```bash
   -XX:MaxRAMPercentage=70.0 -XX:MaxMetaspaceSize=256m
   ```
   For a 2048MB container, max heap is set to ~1433MB, leaving 615MB of headroom for native memory.
2. **Prometheus Alerting**: Configure alerts on cgroup memory pressure:
   ```promql
   container_memory_working_set_bytes{container="order-service"} 
   / container_spec_memory_limit_bytes{container="order-service"} > 0.85
   ```

---

## Production Incident 2: The Shell Form Graceful Shutdown Bypass

### Incident Timeline

| Time | Event |
|---|---|
| **16:00** | Automated CI/CD pipeline triggers a rolling update of the `checkout-service`. |
| **16:02** | Customer payment error rate spikes from 0.01% to 8.5%. Payment transactions fail with `Connection Reset by Peer` and `502 Bad Gateway`. |
| **16:05** | Customers report duplicate credit card charges as retry logic in upstream clients attempts to re-authorize payments that were severed mid-flight. |
| **16:15** | Engineers review logs. Spring Boot's graceful shutdown logs (`Draining active connections...`) are completely absent from terminating pod logs. |

### Root Cause Analysis

Inspection of `checkout-service/Dockerfile`:

```dockerfile
# BROKEN ENTRYPOINT:
ENTRYPOINT java -jar /app/checkout-service.jar
```

The Dockerfile used **shell form**. Inside the container, PID 1 was `/bin/sh`, and the Java process was running as child PID 7:

```text
UID   PID  PPID  C STIME TTY      TIME CMD
app     1     0  0 16:00 ?    00:00:00 /bin/sh -c java -jar /app/checkout-service.jar
app     7     1  8 16:00 ?    00:00:15 java -jar /app/checkout-service.jar
```

When Kubernetes initiated rolling termination, it sent `SIGTERM` to PID 1 (`/bin/sh`). Standard Linux shells do not forward signals to child processes. The Java process never received `SIGTERM`, never knew a shutdown was underway, and never started Spring Boot's graceful shutdown drain.

The pod continued executing until the 30-second `terminationGracePeriodSeconds` expired, at which point Kubernetes sent `SIGKILL` (`kill -9`), brutally murdering the JVM and dropping all active Stripe payment authorizations.

### Remediation & Preventive Measures

1. **Exec Form Entrypoint**:
   ```dockerfile
   ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/checkout-service.jar"]
   ```
   Java now executes as PID 1, intercepts `SIGTERM`, and triggers Spring Boot's graceful shutdown.
2. **Kubernetes Lifecycle Alignment**:
   ```yaml
   lifecycle:
     preStop:
       exec:
         command: ["/bin/sh", "-c", "sleep 5"]
   terminationGracePeriodSeconds: 35
   ```

---

## Essential Metrics and Telemetry

| Metric Name | Type | Description & Alert Threshold |
|---|---|---|
| `container_memory_working_set_bytes` | Gauge | Actual resident memory used by container cgroup. Alert if $> 85\%$ of limit. |
| `container_cpu_cfs_throttled_periods_total` | Counter | Number of scheduling periods where container CPU was throttled. Alert on continuous rise. |
| `jvm.memory.used{area="nonheap"}` | Gauge | Total JVM off-heap allocation (Metaspace, thread stacks, direct buffers). Track alongside heap. |
| `jvm.memory.used{id="Metaspace"}` | Gauge | Metaspace memory consumption. Alert if approaching `-XX:MaxMetaspaceSize`. |
| `spring.boot.actuator.health` | Gauge | Status of container health endpoints (`1: UP`, `0: DOWN/OUT_OF_SERVICE`). |

---

## Production Readiness Checklist

### Dockerfile & Image Optimization
- [ ] Multi-stage build implemented to exclude compilers (`javac`), build tools, and source code from runtime.
- [ ] Base image pinned to minimal headless JRE (`eclipse-temurin:21-jre-alpine` or distroless).
- [ ] Spring Boot layered JARs extracted via `layertools` and copied in order of modification frequency.
- [ ] Dedicated unprivileged user/group created and declared via `USER appuser:appgroup`.
- [ ] Zero secrets or sensitive credentials injected via `ARG` or `ENV` instructions.
- [ ] Docker BuildKit secret mounts (`--mount=type=secret`) used for build-time private dependency resolution.
- [ ] Container image scanned for CVE vulnerabilities using Trivy / Grype in CI/CD pipeline.

### Container Runtime & JVM Ergonomics
- [ ] Explicit container memory limits (`limits.memory`) and CPU quotas (`limits.cpus`) configured in Compose / Kubernetes.
- [ ] JVM configured with dynamic container ergonomics: `-XX:MaxRAMPercentage=70.0` or `75.0`.
- [ ] Metaspace size explicitly capped: `-XX:MaxMetaspaceSize=256m` or `384m`.
- [ ] `-XX:+ExitOnOutOfMemoryError` enabled for fail-fast recovery on internal heap exhaustion.
- [ ] Container `HEALTHCHECK` directive configured and aligned with `/actuator/health/liveness`.
- [ ] Exec form entrypoint used (`ENTRYPOINT ["java", "-jar", "app.jar"]`) or wrapped with `tini` for PID 1 signal propagation.
- [ ] Graceful shutdown configured with adequate drain timeout (`server.shutdown: graceful` and `timeout-per-shutdown-phase: 20s`).
