# Code Review: Resilience Anti-Patterns in Spring Boot 4 & Java 25

!!! info "Delta from baseline"
    Baseline code reviews in [`modules/18-resilience`](../../../topics/resilience/code-review.md) cover configuration errors in Resilience4j YAML files and misconfigured fallback parameters.
    This page presents two review targets showcasing unbounded retry storms and carrier-pinning synchronized circuit breakers under high-concurrency virtual thread workloads.

---

## Review Target 1: Unbounded Retry Storm Without Jitter

### Context
A payment integration service executes retries against a third-party gateway. The retry loop has an unbounded retry count (`Integer.MAX_VALUE`), a fixed static delay of 1000ms with zero jitter, catches and retries fatal client errors (`IllegalArgumentException`), and swallows thread interruptions, causing severe thundering herds and non-cancellable worker execution.

```java
--8<-- "tracks/java25-boot4/modules/18-resilience/broken-examples/unbounded-retry-storm-without-jitter/PaymentGatewayRetryClient.java"
```

??? tip "Review Guidance"
    - Why does retrying with a fixed delay cause thundering herds when a downstream service recovers?
    - Why must fatal input errors (`IllegalArgumentException`) not be retried?
    - What happens to thread lifecycle and cancellation when `InterruptedException` is swallowed?

---

## Review Target 2: Carrier-Pinning Circuit Breaker

### Context
An internal circuit breaker protects downstream RPC calls. The breaker uses synchronized methods to record successes, failures, and check state transitions. Inside the synchronized monitor, it holds locks across long sleeps and state evaluation, pinning carrier OS threads and starving other virtual threads across the JVM.

```java
--8<-- "tracks/java25-boot4/modules/18-resilience/broken-examples/carrier-pinning-circuit-breaker/SynchronizedCircuitBreaker.java"
```

??? tip "Review Guidance"
    - Why does entering a `synchronized` block pin the virtual thread to its carrier OS thread?
    - What happens to carrier thread pools when multiple virtual threads contend on a synchronized monitor?
    - How can `AtomicReference` or `ReentrantLock` eliminate carrier pinning completely?
