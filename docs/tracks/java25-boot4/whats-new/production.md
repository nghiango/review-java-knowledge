# Production: Java 25 & Spring Boot 4 Operational Guide

!!! info "Delta from baseline"
    Baseline modules detail production diagnostics on Java 21 and Spring Boot 3.5.
    This page covers production hazards, diagnostic flags, Flight Recorder events, and pre-upgrade checklists unique to **Java 25 LTS** and **Spring Boot 4.0** deployments.

---

## 1. High-Severity Production Hazards

### Hazard 1: Silent Configuration Drop in Canary Releases
- **Symptom:** Canary instances launch cleanly, HTTP health checks (`/actuator/health`) report `UP`, but outbound messages or downstream events are completely missing.
- **Root Cause:** A property key was renamed in Spring Boot 4. Relaxed binding leaves JavaBean fields as `null`. Methods containing defensive checks like `if (topic == null) return "skipped";` silently drop traffic without error logs or metrics.
- **Mitigation:** Refactor configuration classes to records annotated with `@Validated` and Jakarta Validation constraints (`@NotBlank`, `@NotNull`). A missing property will abort startup immediately.

### Hazard 2: Deadlock from Unsafe `ReentrantLock` Conversions
- **Symptom:** Service latency spikes abruptly; CPU drops to near zero; thread dumps show thousands of virtual threads in `WAITING` state on a single `ReentrantLock`.
- **Root Cause:** A legacy `synchronized` method was refactored to `ReentrantLock` during Java 21 to prevent carrier pinning. An unhandled exception was thrown without a `finally { lock.unlock(); }` block, permanently holding the lock.
- **Mitigation:** In Java 25, `synchronized` unpins cleanly. Revert simple mutual-exclusion locks back to `synchronized` or atomic primitives (`AtomicLong`).

### Hazard 3: Total Security Bypass on Removed SecurityManager
- **Symptom:** Unprivileged users or internal callers successfully invoke administrative endpoints (e.g. data purge or tenant eviction).
- **Root Cause:** Legacy authorization logic relies on `System.getSecurityManager()`. In Java 25 (JEP 486), this method returns `null` permanently. A fail-open check (`if (sm == null) return true;`) grants unrestricted access.
- **Mitigation:** Replace `SecurityManager` calls with domain-level principal authorization guards (`ResourceAuthorizationGuard`) that fail closed.

---

## 2. JVM Diagnostics & Flags

### 1. Simulating `Unsafe` Memory Access Removal
To identify third-party libraries or internal modules still using `sun.misc.Unsafe` before the JVM removes the methods:
```bash
# Deny Unsafe memory access, throwing UnsupportedOperationException on invocation
java --sun-misc-unsafe-memory-access=deny -jar application.jar

# Control warning frequency during staging soak tests
java -XX:+UnlockDiagnosticVMOptions -XX:MaxUnsafeMemoryAccessWarnings=10 -jar application.jar
```

### 2. Monitoring Carrier Thread Pinning
Although `synchronized` is unpinned in Java 25, native calls (JNI) or class initializers `<clinit>` can still pin carrier threads:
```bash
# Print stack traces of pinned virtual threads directly to stdout/stderr
java -Djdk.tracePinnedThreads=full -jar application.jar
```

### 3. Java Flight Recorder (JFR) Profiling
Record pinning events, native allocations, and virtual thread scheduling in production:
```bash
jcmd <PID> JFR.start name=Java25Diagnostics settings=profile.jfc duration=10m filename=java25-prod.jfr
```
Key JFR events to analyze in JDK Mission Control (JMC):
- `jdk.VirtualThreadPinned`: Inspect `duration` and `carrierThread`. If count is non-zero, verify whether third-party JNI libraries are executing blocking I/O.
- `jdk.VirtualThreadSubmitFailed`: Indicates carrier pool saturation or OS resource exhaustion.
- `jdk.NativeMemoryUsage`: Track off-heap growth outside GC heap bounds.

---

## 3. Spring Boot 4 Diagnostic Commands

### Startup Auto-Configuration Evaluation Report
Run with `--debug` to output the Condition Evaluation Report and inspect which auto-configurations were activated or discarded:
```bash
java -jar application.jar --debug | grep -A 5 "Positive matches:"
```

### Inspecting Bound Configuration Properties
Query the Actuator configprops endpoint to verify that environment variables and YAML keys mapped to intended target fields:
```bash
curl -s http://localhost:8080/actuator/configprops | jq '.contexts.application.beans | to_entries[] | select(.key | contains("MessagingSettings"))'
```

---

## 4. Pre-Upgrade Migration Checklist

Before rolling out Java 25 and Spring Boot 4 to production, verify each item:

- [ ] **Phase Isolation:** Split migration into two distinct PRs: Phase 1 (Java 25 runtime on Boot 3.5), Phase 2 (Spring Boot 4 framework upgrade).
- [ ] **Audit `Unsafe`:** Search codebase for `sun.misc.Unsafe` memory-access calls (`grep -rn "sun.misc.Unsafe"`). Migrate all off-heap buffers to `java.lang.foreign.Arena`.
- [ ] **Remove `SecurityManager`:** Search for `System.getSecurityManager()` and `AccessController.checkPermission()`. Verify all authorization paths fail closed.
- [ ] **Audit `ReentrantLock`:** Review manual lock usages introduced for Java 21 virtual thread pinning. Ensure every `lock.lock()` is immediately followed by `try { ... } finally { lock.unlock(); }`, or revert to `synchronized`.
- [ ] **Validate `@ConfigurationProperties`:** Ensure all configuration property classes are Java records annotated with `@Validated` and strict Jakarta Validation constraints (`@NotBlank`, `@Positive`).
- [ ] **Run with Properties Migrator:** Include `spring-boot-properties-migrator` in test environments to detect legacy Boot 3 properties during startup.
- [ ] **Test with Native Memory Tracking (NMT):** Launch staging instances with `-XX:NativeMemoryTracking=summary` to verify deterministic off-heap release via `Arena.close()`.
