# Production: Concurrency Operations & Telemetry in Java 25

!!! info "Delta from baseline"
    Baseline production operations in [`docs/topics/concurrency/production.md`](../../../topics/concurrency/production.md) cover thread pool sizing, thread dumps, and deadlocks.
    This page covers **telemetry, observability, and incident diagnostics in Java 25**: JFR pinning events, carrier starvation triage, and virtual thread monitoring.

---

## 1. Monitoring Virtual Thread Pinning with JFR

While Java 25 unpins virtual threads blocked on `synchronized` ObjectMonitors, JNI calls and native frames can still cause carrier thread pinning:

### Diagnostic Flags
- `-Djdk.tracePinnedThreads=short` (prints a warning and truncated stack trace when a virtual thread pins a carrier thread).
- `-Djdk.tracePinnedThreads=full` (prints a detailed call stack highlighting the pinning native frame).

### JFR Event: `jdk.VirtualThreadPinned`
Configure Flight Recorder in your production profiles:

```xml
<event name="jdk.VirtualThreadPinned">
  <setting name="enabled">true</setting>
  <setting name="stackTrace">true</setting>
  <setting name="threshold">20 ms</setting>
</event>
```

---

## 2. Diagnosing Carrier Pool Starvation Incidents

### Symptoms
1. Application p99/p99.9 latency spikes by orders of magnitude while CPU utilization remains low.
2. Thousands of virtual threads exist in `WAITING` or `TIMED_WAITING` state, but carrier threads (`ForkJoinPool-1-worker-*`) are 100% blocked on external native calls.

### Root Cause Analysis
In Java 21, `synchronized` was the primary culprit. In Java 25, look for:
- Legacy cryptography libraries wrapping C libraries via JNI doing blocking I/O.
- Native database drivers or native compression streams (e.g. gzip/snappy) blocking on network sockets inside JNI boundaries.

### Resolution Strategy
1. Identify pinning native frames via `jdk.tracePinnedThreads=full`.
2. Wrap blocking native calls in a dedicated, bounded OS platform thread pool (`Executors.newFixedThreadPool(...)`) to protect the Loom carrier pool from starvation.
