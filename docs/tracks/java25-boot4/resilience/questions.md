# Resilience Interview Questions: Spring Boot 4 & Java 25

!!! info "Delta from baseline"
    Baseline questions in [`modules/18-resilience`](../../../topics/resilience/questions.md) cover standard Resilience4j annotations, circuit breaker parameters, and rate limiter configurations.
    These 13 questions focus on native resilience pipelines, virtual thread carrier pinning prevention, lock-free circuit breaker mechanics, and full jitter backoffs under Spring Boot 4 and Java 25.

---

### 1. How does modern retry execution compare between native functional backoff and legacy external decorators?

??? question "Reveal answer"
    In Spring Boot 4 and modern Java, programmatic functional pipelines replace complex AOP-based external decorators (like `@Retry`). Instead of wrapping entire beans in dynamic bytecode proxies and relying on rigid annotation configuration, functional execution engines accept lambdas (`Supplier<T>`), inspect specific exception hierarchies, and perform non-blocking exponential backoff directly. This reduces memory footprint, eliminates proxy overhead, and maintains clear stack traces on virtual threads.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/18-resilience/src/examples/java/lab/java25boot4/resilience/questions/Q01CoreRetryVsResilience4jExample.java"
        ```

---

### 2. Why are Semaphore-based bulkheads preferred over thread pool isolation when running on Virtual Threads?

??? question "Reveal answer"
    Thread pool bulkheads assign a dedicated queue and fixed pool of platform threads (e.g. 20 threads) to isolate downstream services. On virtual threads, spawning millions of lightweight threads is trivial, but routing them through a legacy thread pool incurs thread contention, context switching between virtual and platform threads, and queue starvation. A `Semaphore` bulkhead simply acts as an atomic permit counter: virtual threads that fail to acquire a permit park cheaply in user-space without pinning or monopolizing OS carrier threads.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/18-resilience/src/examples/java/lab/java25boot4/resilience/questions/Q02VirtualThreadBulkheadMechanicsExample.java"
        ```

---

### 3. How should timeouts be enforced on Virtual Thread operations without blocking carrier threads?

??? question "Reveal answer"
    Timeouts must never be enforced by blocking platform threads with `Future.get(timeout)` in a thread-pool environment. With modern Java 25 asynchronous pipelines, `CompletableFuture.orTimeout(duration, timeUnit)` or structured task scopes coordinate cancellation asynchronously. When the deadline expires, the task is interrupted or completed with a `TimeoutException`, releasing virtual and carrier thread resources cleanly.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/18-resilience/src/examples/java/lab/java25boot4/resilience/questions/Q03TimeoutHandlingVirtualThreadsExample.java"
        ```

---

### 4. How does a lock-free circuit breaker track failures and transition across CLOSED, OPEN, and HALF_OPEN?

??? question "Reveal answer"
    A lock-free circuit breaker uses `AtomicReference<CircuitState>` and atomic counters (or sliding window buffers) instead of `synchronized` monitors. When failure counts exceed a threshold, a CAS operation flips the state from `CLOSED` to `OPEN`. When subsequent calls arrive after the open duration, a CAS transition moves the state to `HALF_OPEN`, permitting a single trial request to probe service health while rejecting all other concurrent calls with `CircuitBreakerOpenException`.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/18-resilience/src/examples/java/lab/java25boot4/resilience/questions/Q04CircuitBreakerSlidingWindowExample.java"
        ```

---

### 5. What is the mathematical formulation of Full Jitter vs truncated exponential backoff in high-concurrency systems?

??? question "Reveal answer"
    Truncated exponential backoff computes delay as $T = \min(M, B \times 2^{a-1})$. In high-concurrency architectures, all failed requests calculate identical delays and retry simultaneously in lockstep. Full Jitter introduces a uniform random distribution: $\text{delay} = \text{random}(0, \min(M, B \times 2^{a-1}))$. This disperses the retry traffic evenly across the time window, preventing thundering herds from crashing recovering backends.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/18-resilience/src/examples/java/lab/java25boot4/resilience/questions/Q05ExponentialBackoffJitterMathExample.java"
        ```

---

### 6. How does a lock-free token-bucket or atomic permit limiter govern throughput under high concurrency?

??? question "Reveal answer"
    A lock-free rate limiter utilizes atomic CAS operations on a token counter or timestamp reference. Requests attempt to decrement available tokens in a loop (`tokens.compareAndSet(current, current - 1)`). If tokens are zero, requests fail fast or park according to rate limiter policy. Tokens are replenished by background virtual threads or lazily calculated based on elapsed time without taking blocking object locks.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/18-resilience/src/examples/java/lab/java25boot4/resilience/questions/Q06RateLimiterTokenBucketExample.java"
        ```

---

### 7. How should fallback strategies provide deterministic degraded contracts when upstream dependencies fail?

??? question "Reveal answer"
    A fallback strategy must ensure graceful degradation rather than unhandled system failure. Fallbacks can return cached state, static defaults, or queued reconciliation commands. The fallback contract should always be explicit, deterministic, and mapped to domain models or standardized ProblemDetail envelopes so upstream callers know whether data is live or degraded.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/18-resilience/src/examples/java/lab/java25boot4/resilience/questions/Q07FallbackStrategyGracefulDegradationExample.java"
        ```

---

### 8. How does synchronized blocking inside resilience decorators trigger carrier thread pinning on Virtual Threads?

??? question "Reveal answer"
    In Java, entering a `synchronized` block binds the virtual thread to its underlying carrier OS thread. If any blocking operation—such as socket I/O, `Thread.sleep` during backoff, or lock acquisition contention—occurs inside that synchronized block, the virtual thread cannot unmount. This pins the carrier thread. Under high load, all ForkJoinPool carrier threads become pinned, stalling the entire application. Replacing `synchronized` with `ReentrantLock` or atomic CAS eliminates carrier pinning.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/18-resilience/src/examples/java/lab/java25boot4/resilience/questions/Q08CarrierPinningResilienceTrapExample.java"
        ```

---

### 9. How do you migrate heavy external Resilience4j decorators to modern Spring Boot 4 lightweight execution pipelines?

??? question "Reveal answer"
    Migration involves removing external resilience annotations (`@Retry`, `@CircuitBreaker`) and their corresponding CGLIB proxy configurations. Instead, define lightweight Spring beans encapsulating `ModernResilientExecutionEngine` or functional pipeline wrappers around target clients. Configure retry predicates and timeout durations as immutable configuration properties, avoiding legacy annotation magic and improving unit testability.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/18-resilience/src/examples/java/lab/java25boot4/resilience/questions/Q09MigrationResilience4jToCoreSpringExample.java"
        ```

---

### 10. How does replacing fixed-delay retries with Full Jitter prevent the thundering herd problem during service recovery?

??? question "Reveal answer"
    When 500 requests fail simultaneously due to a network glitch, fixed-delay retries schedule all 500 retry calls at $t + 1000\text{ms}$. This sudden surge overwhelms the recovering database or API. Full Jitter redistributes each retry across the entire interval between 0 and the exponential maximum. The arrival rate becomes uniform, spreading load smoothly and allowing downstream recovery.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/18-resilience/src/examples/java/lab/java25boot4/resilience/questions/Q10MigrationFixedDelayToFullJitterExample.java"
        ```

---

### 11. How do Virtual Threads handle thousands of concurrent resilient executions without thread pool exhaustion?

??? question "Reveal answer"
    Virtual threads consume only a few hundred bytes of heap memory compared to ~1MB for platform OS threads. By combining virtual-thread-per-task executors with non-blocking concurrency limiters and lock-free circuit breakers, applications can handle tens of thousands of concurrent client requests without risking `OutOfMemoryError: unable to create native thread` or thread pool queue exhaustion.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/18-resilience/src/examples/java/lab/java25boot4/resilience/questions/Q11HighThroughputVirtualThreadStressExample.java"
        ```

---

### 12. How do you capture and publish Micrometer metrics and OpenTelemetry trace tags across retry and circuit breaker transitions?

??? question "Reveal answer"
    Resilience metrics should be captured via Micrometer counters, timers, and gauges: tracking retry attempt counts, circuit breaker state changes (`CLOSED` -> `OPEN`), and fallback executions. With OpenTelemetry, every retry attempt should register as an event or span attribute (`retry.attempt=1`, `resilience.circuit_state=OPEN`), ensuring distributed traces accurately display transient failures and backoff latencies.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/18-resilience/src/examples/java/lab/java25boot4/resilience/questions/Q12ObservabilityResilienceMetricsExample.java"
        ```

---

### 13. In an incident where downstream payment gateway latency spikes to 30s, how does a modern circuit breaker prevent cascading thread starvation?

??? question "Reveal answer"
    When the payment gateway latency spikes to 30s, calls begin timing out and failing. Once the failure threshold is reached (e.g. 3 consecutive failures), the circuit breaker trips to `OPEN`. Subsequent calls fail fast immediately (in microseconds) with `CircuitBreakerOpenException` without waiting for the 30-second timeout. This prevents client requests from piling up, preserves system memory, and protects other independent services from thread starvation.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/18-resilience/src/examples/java/lab/java25boot4/resilience/questions/Q13ScenarioCascadingPaymentOutageExample.java"
        ```
