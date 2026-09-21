# Production Incident: Cascading Outage and Carrier Pinning Collapse

!!! info "Delta from baseline"
    Baseline production scenarios in [`modules/18-resilience`](../../../topics/resilience/production.md) cover standard timeout misconfigurations and database pool exhaustion under Resilience4j.
    This incident demonstrates how fixed-delay retries combined with synchronized circuit breaker monitors trigger cascading service failure and carrier thread starvation across virtual threads.

---

## 1. Incident Timeline

- **09:14:00 UTC**: Downstream third-party payment gateway latency spikes from 120ms to 28 seconds due to database lock contention.
- **09:14:15 UTC**: Upstream checkout services begin timing out. Because retries are configured with static fixed delay (1000ms) and zero jitter, thousands of concurrent virtual threads retry in synchronized lockstep every second.
- **09:14:30 UTC**: Downstream gateway enters a thundering herd crash cycle: every recovery attempt is immediately squashed by waves of thousands of concurrent retries hitting at $t+1000\text{ms}$.
- **09:14:45 UTC**: Checkout service's internal circuit breaker attempts to trip. However, because the circuit breaker uses `synchronized` methods for state recording, hundreds of virtual threads contend on the object monitor.
- **09:15:00 UTC**: The JVM's ForkJoinPool carrier threads become pinned to the contended synchronized monitors. With all carrier threads pinned, new HTTP requests cannot be scheduled. The entire checkout cluster becomes completely unresponsive.

---

## 2. Root Cause Analysis

```text
[Downstream Latency Spike]
          │
          ▼
[Fixed-Delay Retries (No Jitter)] ───► [Thundering Herd Wave Hits Gateway Every 1s]
          │
          ▼
[Contended Synchronized Monitor] ───► [Pins All ForkJoinPool Carrier OS Threads]
          │
          ▼
[Complete JVM Thread Starvation & Cluster Collapse]
```

1. **Lack of Jitter**: Fixed retry intervals caused retry synchronization, creating artificial peak loads that prevented downstream recovery.
2. **Carrier Thread Pinning**: Virtual threads running inside `synchronized` blocks could not unmount when awaiting lock acquisition or I/O, exhausting carrier threads.
3. **No Load Shedding**: Concurrency was unconstrained; no bulkhead or load shedding rejected excess requests early.

---

## 3. Remediation & Verification

1. **Replace Fixed Retries with Full Jitter**: Adopt `ModernResilientExecutionEngine.calculateJitteredBackoff`, dispersing retries uniformly across $[0, \min(\text{max}, \text{base} \times 2^{\text{attempt}})]$.
2. **Migrate to Lock-Free Circuit Breaker**: Replace `synchronized` with `AtomicReference<CircuitState>`, allowing virtual threads to inspect and transition states without pinning carriers.
3. **Enforce Concurrency Limit**: Add an atomic concurrency gate (`maxConcurrency = 50`) to fast-fail excess requests with `ConcurrencyLimitExceededException` before calling the downstream gateway.
