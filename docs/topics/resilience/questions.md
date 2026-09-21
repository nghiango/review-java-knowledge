# Resilience Interview Questions

Answer each question before expanding its explanation.

<!-- --8<-- [start:basic] -->
## Basic Concepts

### What is the timeout hierarchy in distributed systems, and why must connect, read, and execution timeouts be configured independently?

Explain the difference between TCP SYN handshakes, idle socket packet gaps, and total end-to-end operation deadlines.

??? question "Reveal answer"
    - **Connect Timeout**: Time spent completing the TCP 3-way handshake or TLS negotiation. Fast failure (e.g. 500ms) detects downed hosts or network black holes without holding caller threads.
    - **Read Timeout (Socket Timeout)**: Maximum inactivity delay between incoming network packets on an established TCP stream. Protects against remote server hangs during payload generation.
    - **Execution Timeout (TimeLimiter)**: End-to-end deadline capping total wall-clock duration of an operation across multiple network hops, internal processing, and retry attempts.
    - **Independent Configuration**: Setting only a read timeout allows infinite connect hangs; setting only an execution timeout fails to free underlying socket resources if cancellation is uncooperative.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q01TimeoutHierarchyConnectReadExecutionExample.java"
    ```

---

### What is retry amplification, and how does multi-tier retrying create downstream catastrophic outages?

Detail how client, gateway, and service retry multipliers compound geometrically.

??? question "Reveal answer"
    - **Multiplicative Blast Radius**: If client, API gateway, and intermediate microservices all implement 3 retries, a single user request generates $3 \times 3 \times 3 = 27$ downstream calls.
    - **Failure Amplification**: Under partial downstream degradation, retry amplification converts a small 10% traffic increase into an overwhelming 270% surge.
    - **Remediation**: Limit retries to a single layer in the request path (typically the immediate caller or edge proxy), use retry budgets (e.g., maximum 10% retry traffic), and employ Circuit Breakers to truncate retries immediately upon widespread failure.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q02RetryAmplificationAndStormsExample.java"
    ```

---

### How do Exponential Backoff and Randomized Jitter prevent thundering herd retry storms?

Compare deterministic exponential backoff ($2^n$) with Full Jitter and Equal Jitter algorithms.

??? question "Reveal answer"
    - **Deterministic Backoff Flaw**: Without jitter, failed concurrent requests sleep for identical durations ($500\text{ms}, 1000\text{ms}, 2000\text{ms}$) and retry simultaneously in synchronized waves, hammering downstream services.
    - **Full Jitter**: Calculates exponential interval $T = \text{base} \times 2^{\text{attempt}-1}$, then selects a uniform random delay between $0$ and $T$. AWS architectural research demonstrates Full Jitter achieves the lowest contention and shortest recovery time.
    - **Equal Jitter**: Divides backoff into a deterministic half and a randomized half: $\frac{T}{2} + \text{random}(0, \frac{T}{2})$, guaranteeing a minimum backoff duration.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q03ExponentialBackoffAndJitterStrategiesExample.java"
    ```

---

### What are the three primary states of a Circuit Breaker, and what triggers transitions among them?

Walk through CLOSED, OPEN, and HALF_OPEN states and their failure thresholds.

??? question "Reveal answer"
    - **CLOSED**: Normal state. Invocations execute directly against the remote dependency and outcomes are recorded in a sliding window.
    - **CLOSED $\to$ OPEN**: Triggered when either the failure rate or slow call rate exceeds configured percentage thresholds within the sliding window, provided minimum call volume is met.
    - **OPEN $\to$ HALF_OPEN**: After `waitDurationInOpenState` elapses, the circuit breaker enters trial mode, permitting a bounded number of probe calls (`permittedNumberOfCallsInHalfOpenState`).
    - **HALF_OPEN $\to$ CLOSED**: If probe calls achieve a success rate above the threshold, the circuit closes and normal traffic resumes.
    - **HALF_OPEN $\to$ OPEN**: If probe calls fail, the circuit returns to OPEN for another cooldown period.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q04CircuitBreakerStateTransitionsExample.java"
    ```

---

### Compare Count-Based vs Time-Based sliding windows in Resilience4j.

Analyze memory footprint, traffic sensitivity, and cold-start edge cases.

??? question "Reveal answer"
    - **Count-Based Window**: Evaluates the outcome of the last $N$ calls (e.g. 100 calls) using a ring bit-set. Highly responsive under steady high-throughput traffic, but under low traffic, stale failures from hours ago can persist in the window.
    - **Time-Based Window**: Evaluates outcomes across the last $N$ seconds divided into circular 1-second buckets. Automatically purges old failure metrics regardless of request rate, making it superior for bursty or low-frequency services.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q05SlidingWindowCountVsTimeBasedExample.java"
    ```

---

### What is the difference between a Semaphore Bulkhead and a ThreadPool Bulkhead?

Evaluate thread switching overhead, resource isolation guarantees, and compatibility with Java 21 Virtual Threads.

??? question "Reveal answer"
    - **Semaphore Bulkhead**: Uses an atomic counter or semaphore to limit concurrent executions on the calling thread. Incurs zero context-switching overhead and is the idiomatic isolation model for Java 21 Virtual Threads (Project Loom).
    - **ThreadPool Bulkhead**: Executes calls asynchronously on a dedicated, bounded `ThreadPoolExecutor` with a bounded queue. Provides complete CPU and thread pool isolation at the cost of thread context-switching and carrier thread overhead.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q06BulkheadSemaphoreVsThreadPoolExample.java"
    ```

---

### How do Token Bucket and Leaky Bucket rate limiting algorithms differ in handling bursty traffic?

Explain token replenishment rates, bucket capacity, and traffic smoothing.

??? question "Reveal answer"
    - **Token Bucket**: Tokens are continuously added to a bucket up to capacity. Invocations consume tokens. Allows instant bursts of traffic up to the bucket capacity while maintaining an average rate over time.
    - **Leaky Bucket**: Requests enter a queue and leak out at a strictly constant, smoothed rate regardless of incoming burst volume. Smooths out traffic spikes completely, but introduces queue latency for bursty callers.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q07RateLimiterTokenBucketVsLeakyBucketExample.java"
    ```

---

### What is the distinction between Rate Limiting and Load Shedding?

Contrast client-side contractual constraints with server-side survival mechanisms.

??? question "Reveal answer"
    - **Rate Limiting**: Client-focused traffic governance enforcing contractual usage quotas (e.g. 100 req/min per API key) regardless of whether the server is idle or saturated.
    - **Load Shedding**: Server-centric survival mechanism. When system health indicators (CPU utilization, queue latency, GC pause time) exceed safe thresholds, the server drops low-priority or non-critical requests to maintain throughput and latency for high-value transactions.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q08LoadSheddingVsRateLimitingExample.java"
    ```
<!-- --8<-- [end:basic] -->

---

<!-- --8<-- [start:intermediate] -->
## Intermediate Patterns

### What fallback patterns are commonly used during partial failures, and what are their trade-offs?

Detail cached stale data, default stubs, fail-silent patterns, and asynchronous compensation.

??? question "Reveal answer"
    - **Stale Cache Read**: Return previously cached responses from a local Caffeine or Redis cache. Provides realistic data at the risk of serving outdated prices or inventory numbers.
    - **Default Stub**: Return neutral default values (e.g. empty recommendations list, generic greeting). Harmless for auxiliary features, but unacceptable for core financial transactions.
    - **Fail-Silent**: Drop non-critical operations (e.g. logging user activity to an analytics service) without throwing exceptions to upstream callers.
    - **Compensating Queue**: Buffer the mutation in a local durable outbox or dead-letter queue for eventual asynchronous reconciliation.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q09FallbackPatternsAndGracefulDegradationExample.java"
    ```

---

### What is the execution order of Resilience4j aspects when multiple annotations are applied?

Analyze the layering: `@Retry`, `@CircuitBreaker`, `@RateLimiter`, `@TimeLimiter`, and `@Bulkhead`.

??? question "Reveal answer"
    - **Default Spring AOP Aspect Order (Outer to Inner)**:
        $\displaystyle \text{Retry} \to \text{CircuitBreaker} \to \text{RateLimiter} \to \text{TimeLimiter} \to \text{Bulkhead}$
    - **Retry Wraps CircuitBreaker**: If an attempt fails, Retry retries it. CircuitBreaker records each individual attempt, allowing the circuit to open quickly if repeated retries fail.
    - **CircuitBreaker Wraps RateLimiter**: When the circuit is OPEN, calls are rejected immediately before consuming client rate limiter permits.
    - **TimeLimiter Wraps Bulkhead**: When an execution times out, TimeLimiter cancels the invocation and frees the bulkhead permit.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q10Resilience4jAspectOrderDecoratorsExample.java"
    ```

---

### Why is retrying non-idempotent HTTP POST requests hazardous, and how do Idempotency Keys resolve the issue?

Diagnose socket read timeout ambiguity and duplicate charge risks.

??? question "Reveal answer"
    - **Socket Read Timeout Ambiguity**: A socket read timeout indicates that the client sent the HTTP request and TCP established successfully, but the server took longer than the deadline to reply. The server may have already committed the payment in its database before the connection was severed.
    - **Duplicate Risk**: Retrying without deduplication sends a second payment request, charging the customer twice.
    - **Idempotency Keys**: The client generates a unique UUID `Idempotency-Key` header per business transaction. The downstream payment gateway checks its deduplication store: if the key exists, it returns the cached confirmation response rather than re-executing the charge.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q11IdempotencyInRetryingPostCallsExample.java"
    ```

---

### How do you classify transient vs permanent exceptions to avoid retrying poison pills?

Contrast HTTP 503/504 against 400 Bad Request and 402 Payment Required.

??? question "Reveal answer"
    - **Transient Exceptions (Retryable)**: Network timeouts, connection resets, HTTP 503 Service Unavailable, 504 Gateway Timeout, and HTTP 429 Too Many Requests. These represent temporary blips that may succeed on subsequent attempts.
    - **Permanent Exceptions (Non-Retryable)**: HTTP 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 422 Unprocessable Entity, and domain exceptions (e.g. `InsufficientFundsException`). Retrying permanent errors is futile, wastes CPU and network bandwidth, and delays reporting errors to users.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q12PoisonPillsAndNonRetryableExceptionsExample.java"
    ```

---

### How does Resilience4j's slowCallRateThreshold operate, and why is slow call detection as critical as error rate detection?

Explain slow call duration thresholds, latency degradation, and thread pool starvation.

??? question "Reveal answer"
    - **Mechanism**: Calls taking longer than `slowCallDurationThreshold` are tagged as "slow calls". If the percentage of slow calls exceeds `slowCallRateThreshold` (e.g. 50%) across the sliding window, the circuit breaker trips to OPEN.
    - **Why It Matters**: Slow calls that eventually succeed (HTTP 200 after 15s) are more dangerous than instant HTTP 500 errors! Instant errors return immediately and release threads; slow calls monopolize container worker threads, causing thread pool starvation and cascading service paralysis.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q13CircuitBreakerSlowCallRateThresholdExample.java"
    ```

---

### How do Circuit Breakers prevent cascading failures across a distributed microservice mesh?

Detail fast-failure dynamics, thread release, and downstream recovery breathing room.

??? question "Reveal answer"
    - **Cascading Failure Cycle**: Service A calls Service B. Service B degrades. Service A threads block waiting for B. Service A exhausts its Tomcat thread pool, causing Service A to fail health checks. Upstream Service Gateway fails trying to call A, cascading failure across the entire infrastructure.
    - **Circuit Breaker Intervention**: When Service B degrades, Service A's Circuit Breaker trips to OPEN. Subsequent calls fail immediately in $0\text{ms}$ with `CallNotPermittedException`. Service A threads remain free to serve other traffic, and Service B is relieved of load, enabling it to recover.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q14CascadingFailuresAndCircuitBreakerMeshExample.java"
    ```

---

### How does Resilience4j TimeLimiter interact with CompletableFuture and asynchronous scheduling?

Explain task cancellation, daemon schedulers, and future thread interruption.

??? question "Reveal answer"
    - **Mechanism**: `TimeLimiter` decorates a `CompletionStage` or `Future`. It schedules a timeout task on an internal `ScheduledExecutorService`.
    - **Timeout Expiration**: If the wrapped future does not complete before the timeout duration, the scheduler triggers `TimeoutException` and optionally invokes `future.cancel(true)` if `cancelRunningFuture` is enabled.
    - **Interruption Caveat**: Calling `future.cancel(true)` sends an interrupt to the worker thread. If the underlying code performs uninterruptible I/O, the thread continues running in the background until the socket read timeout triggers.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q15TimeLimiterWithCompletableFutureExample.java"
    ```

---

### How do Adaptive Concurrency Limits (e.g. Netflix Concurrency Limits) improve upon static thread pool and bulkhead sizes?

Analyze Little's Law, queueing delay detection, and dynamic Vegas algorithms.

??? question "Reveal answer"
    - **Static Limit Flaw**: Hardcoding a bulkhead to 50 concurrent requests causes queuing during peak load and fails to adapt when downstream capacity changes dynamically (e.g. database failover to smaller replica).
    - **Adaptive Concurrency**: Derived from Little's Law ($L = \lambda W$) and TCP congestion algorithms (TCP Vegas, AIMD). Dynamically measures round-trip latency ($RTT$). If $RTT$ rises above baseline without throughput gains, the limiter infers queueing delay and throttles concurrency down to prevent bufferbloat.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q16AdaptiveConcurrencyLimitsNetflixConcurrencyLimitsExample.java"
    ```
<!-- --8<-- [end:intermediate] -->

---

<!-- --8<-- [start:senior] -->
## Senior Engineering

### What Micrometer metrics and Prometheus alerting rules are essential for monitoring Resilience4j in production?

Examine state gauges, call counters, retry distribution, and alert query formulas.

??? question "Reveal answer"
    - **Core Metrics**:
        - `resilience4j.circuitbreaker.state`: Gauge with tag `state=closed|open|half_open`.
        - `resilience4j.circuitbreaker.calls`: Counter tagged by `kind=successful|failed|not_permitted`.
        - `resilience4j.circuitbreaker.failure.rate`: Current percentage of failed calls in the sliding window.
        - `resilience4j.retry.calls`: Counter tagged by `kind=successful_without_retry|successful_with_retry|failed_with_retry`.
    - **Prometheus Alerting**:
        - Alert when `resilience4j_circuitbreaker_state{state="open"} == 1` for $> 1\text{m}$.
        - Alert when retry rate exceeds 15% of total calls: `rate(resilience4j_retry_calls_total[5m]) / rate(http_requests_total[5m]) > 0.15`.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q17MicrometerMetricsForResilience4jExample.java"
    ```

---

### How do Java 21 Virtual Threads affect resilience patterns and bulkhead designs?

Analyze carrier thread pinning, unbounded thread creation risks, and Semaphore vs ThreadPool bulkheads.

??? question "Reveal answer"
    - **ThreadPool Bulkheads Obsolete**: Virtual threads make custom thread pool bulkheads unnecessary and counter-productive. Managing dedicated thread pools introduces carrier thread context-switching and scheduler overhead.
    - **Semaphore Bulkheads Essential**: Because virtual threads are lightweight ($<1\text{KB}$), an unbounded server can spawn 500,000 virtual threads that overwhelm downstream databases and socket connections. `SemaphoreBulkhead` is the proper pattern: it limits concurrency to downstream capacity without consuming operating system threads.
    - **Pinning Warning**: Ensure resilience libraries do not execute `synchronized` blocks around blocking socket I/O to avoid pinning carrier threads.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q18VirtualThreadsImpactOnBulkheadsExample.java"
    ```

---

### In security and resilience architecture, when must a system Fail-Closed versus Fail-Open?

Contrast authorization, fraud detection, feature flags, and UI personalization.

??? question "Reveal answer"
    - **Fail-Closed (Deny by Default)**: Mandatory for authentication, authorization, cryptographic verification, and financial balance validation. If the IAM or policy decision point (PDP) is unreachable, the system must deny access. Failing open in security creates catastrophic data breaches.
    - **Fail-Open (Permit by Default)**: Appropriate for non-critical features: personalization widgets, product recommendations, analytics tracking, and non-essential fraud risk scores where false positives harm user checkout conversion more than rare fraud loss.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q19FailOpenVsFailClosedSecurityResilienceExample.java"
    ```

---

### How do Hedged Requests (The Tail at Scale) eliminate p99 latency spikes in distributed queries?

Explain duplicate speculative requests, cancellation tokens, and cluster load trade-offs.

??? question "Reveal answer"
    - **Concept**: Introduced in Jeff Dean's "The Tail at Scale". A client sends a request to one replica. If no response is received within the expected p95 latency (e.g. 150ms), a second speculative copy ("hedge") is sent to another replica.
    - **Resolution**: Whichever replica responds first completes the operation; the client immediately cancels or ignores the second request.
    - **Trade-off**: Drastically eliminates tail p99 latency caused by background garbage collection or bad disk sectors on one replica, at the cost of adding a modest 5% total request volume to the cluster.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q20HedgedRequestsAndTailLatencyTolerantExample.java"
    ```

---

### Compare in-memory Resilience4j Rate Limiting with distributed Redis Token Bucket rate limiting.

Evaluate latency overhead, cluster-wide quota fairness, race conditions, and single points of failure.

??? question "Reveal answer"
    - **Resilience4j RateLimiter (Local)**: In-memory, lock-free, sub-microsecond latency ($<1\mu\text{s}$). However, it limits rates per-JVM instance. With 20 microservice pods, a 100 req/sec limit allows 2,000 req/sec across the cluster.
    - **Redis RateLimiter (Distributed)**: Coordinates global state using Redis Lua scripts (Token Bucket or Sliding Window Log). Enforces exact cluster-wide quotas across all pods, but incurs a 1ms network round-trip overhead and introduces Redis as a critical availability dependency.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q21DistributedRateLimitingRedisVsResilience4jExample.java"
    ```
<!-- --8<-- [end:senior] -->

---

<!-- --8<-- [start:scenarios] -->
## Production Incidents & Scenarios

### Production incident post-mortem: How an outage at a downstream payment gateway caused a synchronized retry storm that knocked the entire application offline.

Diagnose the thundering herd, absence of jitter, thread pool exhaustion, and remediation steps.

??? question "Reveal answer"
    - **Incident Timeline**: A downstream payment gateway suffered a 30-second network blip. The checkout service retried failed calls using fixed exponential backoff ($500\text{ms}, 1000\text{ms}, 2000\text{ms}$) without jitter. When the gateway attempted to restart, 15,000 pending checkout threads fired simultaneous HTTP requests in the exact same millisecond. The gateway crashed again immediately under the surge.
    - **Tomcat Exhaustion**: The retry loops monopolized all 200 worker threads in every Tomcat container. Health check endpoints (`/actuator/health`) timed out, causing Kubernetes to restart all pods in an unrecoverable crash loop.
    - **Remediation**:
        1. Applied Resilience4j `IntervalFunction.ofExponentialRandomBackoff` with Full Jitter.
        2. Configured Circuit Breakers to open and fast-fail checkout calls when error rate exceeded 50%.
        3. Separated health checks into a dedicated management port with an isolated server thread pool.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q22IncidentDownstreamOutageRetryStormExample.java"
    ```

---

### Production incident post-mortem: A misconfigured Circuit Breaker became permanently stuck in the OPEN state, refusing traffic for 6 hours after downstream recovery.

Examine permittedNumberOfCallsInHalfOpenState, minimumNumberOfCalls, and health check validation.

??? question "Reveal answer"
    - **Incident Timeline**: A downstream service recovered from maintenance, but the upstream Circuit Breaker remained stuck in `OPEN` state, rejecting 100% of user traffic for 6 hours.
    - **Root Cause**: The configuration had `permittedNumberOfCallsInHalfOpenState: 10`, but also defined `minimumNumberOfCalls: 50`. In HALF_OPEN state, Resilience4j only permits 10 trial calls. Because the minimum calls required to compute the sliding window was 50, the circuit breaker could never accumulate enough calls to calculate a failure rate, remaining stuck in HALF_OPEN or reverting to OPEN.
    - **Remediation**:
        1. Ensured `permittedNumberOfCallsInHalfOpenState` matches or exceeds the sample size needed to evaluate health in half-open state (typically 3 to 10 calls).
        2. Added automated alerting on `resilience4j_circuitbreaker_state{state="open"} > 15m`.
        3. Exposed Spring Boot Actuator endpoint `/actuator/circuitbreakers` to allow on-call engineers to manually transition or reset stuck circuit breakers in emergency situations.

??? example "Example"
    ```java
    --8<-- "modules/18-resilience/src/examples/java/lab/resilience/questions/Q23IncidentCircuitBreakerStuckOpenStateExample.java"
    ```
<!-- --8<-- [end:scenarios] -->
