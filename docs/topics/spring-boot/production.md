# Spring Boot in Production: Operational Guide

Hardening Spring Boot services for cloud and Kubernetes deployments.

---

## 1. Actuator Security Checklist

| Check | Recommendation | Production Configuration |
|---|---|---|
| **Endpoint Whitelist** | Expose only required endpoints | `management.endpoints.web.exposure.include: "health,info,metrics,prometheus"` |
| **Port Isolation** | Bind Actuator to internal port | `management.server.port: 9090` |
| **Environment Masking** | Never reveal plaintext secrets | `management.endpoint.env.show-values: never` |
| **Disable Shutdown** | Prevent remote process termination | `management.endpoint.shutdown.enabled: false` |
| **Health Details** | Restrict infrastructure metadata | `management.endpoint.health.show-details: when_authorized` |

---

## 2. Kubernetes Health Probes Architecture

Never conflate Kubernetes **Liveness** with **Readiness**:

```mermaid
flowchart TD
    subgraph LivenessProbe [/actuator/health/liveness]
        LiveState[Liveness State] --> Ping[Ping Indicator]
        LiveState --> JVM[JVM Alive & Non-Deadlocked]
    end

    subgraph ReadinessProbe [/actuator/health/readiness]
        ReadyState[Readiness State] --> Ping2[Ping]
        ReadyState --> DB[(Database Connectivity)]
        ReadyState --> Redis[(Redis Cache)]
        ReadyState --> Broker[(Kafka / RabbitMQ Broker)]
    end

    K8s[Kubernetes Kubelet] -->|Fails: Kills & Restarts Pod| LivenessProbe
    K8s -->|Fails: Removes Pod from Service Endpoints| ReadinessProbe
```

### Kubernetes Pod Spec Best Practice

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 9090
  initialDelaySeconds: 15
  periodSeconds: 10
  timeoutSeconds: 3
  failureThreshold: 3
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 9090
  initialDelaySeconds: 10
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 2
lifecycle:
  preStop:
    exec:
      command: ["/bin/sh", "-c", "sleep 10"]
```

---

## 3. Graceful Shutdown & Pod Termination

During a rolling deployment in Kubernetes:

1. Pod enters `Terminating` state.
2. Ingress controller receives endpoint removal event.
3. `preStop` hook sleeps for 10 seconds, allowing in-flight requests on the wire to complete and ingress routing tables to update.
4. Kubernetes sends `SIGTERM` to the Spring Boot process.
5. Spring Boot stops accepting new HTTP connections and waits up to `spring.lifecycle.timeout-per-shutdown-phase` (e.g. 20 seconds) for active requests and background jobs to finish.
6. JVM terminates gracefully with exit code 0.

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 20s
```

---

## 4. Startup Diagnostics & Performance Optimization

### Diagnose Auto-Configuration Decisions
Run with `--debug` or analyze startup condition evaluation reports:

```bash
java -jar app.jar --debug
```

### Enable Lazy Initialization (for Test / Dev Speed)
```properties
spring.main.lazy-initialization=true
```

### Virtual Threads Configuration (Java 21+)
```yaml
spring:
  threads:
    virtual:
      enabled: true
```
Enables virtual thread per task executors for embedded Tomcat and `@Async` tasks, scaling concurrent blocking I/O calls without pool exhaustion.
