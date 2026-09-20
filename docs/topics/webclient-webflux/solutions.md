# WebClient & WebFlux Solutions

This section explains the production-grade solutions for the 5 broken review examples, detailing architectural rationale, concurrency mechanics, and operational trade-offs.

---

## 1. Non-Blocking Request Flows (Avoiding `.block()`)

<a id="non-blocking-request-flows-avoiding-block"></a>

### Implementation Comparison

=== "Broken"
    ```java
    --8<-- "modules/21-webclient-webflux/broken-examples/block-in-request-flow/ReactivePaymentGateway.java"
    ```

=== "Correct"
    ```java
    --8<-- "modules/21-webclient-webflux/src/main/java/lab/webflux/blockinflow/CorrectReactivePaymentGateway.java"
    ```

### Why the Solution Works

1. **Continuous Asynchronous Pipeline**: By replacing `.block()` with `.flatMap()`, the calling thread is released as soon as the initial HTTP GET request is dispatched over Netty's event loop channel. When the bank service responds with token validation bytes, Netty triggers the flatMap callback asynchronously.
2. **Backpressure & Cancellation Propagation**: If the end-user closes their browser tab or upstream client disconnects, the cancellation signal flows backward through `.flatMap()` to cancel the downstream bank charge before execution.
3. **Graceful Empty State Handling**: Using `.defaultIfEmpty(...)` safely catches scenarios where token validation returns an empty response without triggering null pointer exceptions.

### Trade-offs
- Functional composition requires disciplined error handling: errors inside `.flatMap()` must be intercepted via `.onErrorResume()` rather than traditional `try/catch` blocks.
- Stack traces span multiple asynchronous boundaries, requiring Reactor Debug Agent or Micrometer Observation for trace correlation.

---

## 2. Offloading Blocking JDBC to `boundedElastic`

<a id="offloading-blocking-jdbc-to-boundedelastic"></a>

### Implementation Comparison

=== "Broken"
    ```java
    --8<-- "modules/21-webclient-webflux/broken-examples/blocking-jdbc-in-webflux/CustomerSummaryService.java"
    ```

=== "Correct"
    ```java
    --8<-- "modules/21-webclient-webflux/src/main/java/lab/webflux/blockingjdbc/CorrectCustomerSummaryService.java"
    ```

### Why the Solution Works

1. **Thread Pool Isolation**: `subscribeOn(Schedulers.boundedElastic())` moves the execution of `Mono.fromCallable(...)` off the Netty event loop (`reactor-http-epoll-*`) and onto a dedicated worker thread pool. Netty event loops remain free to handle thousands of concurrent network connections.
2. **Bounded Worker Capacity**: Unlike the deprecated `Schedulers.elastic()` (which spawned unbounded threads and caused OutOfMemoryErrors), `boundedElastic()` caps its worker pool at $10 \times \text{CPU cores}$ with a maximum queue capacity of 100,000 tasks.
3. **Hard Query Deadline**: Adding `.timeout(Duration.ofSeconds(3))` ensures that slow database queries or locked tables abort cleanly rather than tying up worker threads indefinitely.

### Trade-offs
- Offloading to worker threads incurs OS thread context switching overhead.
- Relational database connections in HikariCP must be sized in coordination with the `boundedElastic()` pool size to avoid connection pool starvation.

---

## 3. Bounded `flatMap` Concurrency and Error Isolation

<a id="bounded-flatmap-concurrency-and-error-isolation"></a>

### Implementation Comparison

=== "Broken"
    ```java
    --8<-- "modules/21-webclient-webflux/broken-examples/uncontrolled-flatmap-concurrency/NotificationBatchSender.java"
    ```

=== "Correct"
    ```java
    --8<-- "modules/21-webclient-webflux/src/main/java/lab/webflux/flatmapconcurrency/CorrectNotificationBatchSender.java"
    ```

### Why the Solution Works

1. **Controlled Concurrency Limit**: Specifying `flatMap(mapper, maxConcurrency)` (e.g., concurrency = 16 or 32) restricts the number of active, in-flight HTTP requests. Upstream items are pulled into the pipeline only as earlier requests complete.
2. **Preventing Connection Pool Exhaustion**: Limiting concurrency ensures the total active socket requests stay comfortably below Reactor Netty's default connection pool ceiling (500 connections), eliminating `PoolAcquireTimeoutException`.
3. **Inner Publisher Error Isolation**: Chaining `.onErrorResume(...)` inside the inner lambda prevents an error on a single message from aborting the entire outer `Flux`. Failed messages are recorded as `delivered=false` while the rest of the batch continues.

### Trade-offs
- Lower concurrency bounds increase total batch execution wall-clock time. Tune `maxConcurrency` according to downstream rate limits and network latency.

---

## 4. Configuring WebClient Timeouts

<a id="configuring-webclient-timeouts"></a>

### Implementation Comparison

=== "Broken"
    ```java
    --8<-- "modules/21-webclient-webflux/broken-examples/missing-webclient-timeout/CarrierTrackingClient.java"
    ```

=== "Correct"
    ```java
    --8<-- "modules/21-webclient-webflux/src/main/java/lab/webflux/missingtimeout/CorrectCarrierTrackingClient.java"
    ```

### Why the Solution Works

1. **Multi-Layer Timeout Strategy**:
   - **Netty Channel Level**: Setting `ChannelOption.CONNECT_TIMEOUT_MILLIS` (e.g. 500ms) enforces rapid failure if the remote host drops SYN packets.
   - **Netty Response Level**: Setting `HttpClient.create().responseTimeout(timeout)` terminates the socket if response body packets stall.
   - **Stream Operator Level**: Chaining `.timeout(timeout)` on the returned `Mono` provides a strict business SLA deadline.
2. **Resilient Fallback**: `.onErrorResume(TimeoutException.class, ...)` intercepts timeouts and returns a degraded `TrackingInfo` object (`status = UNAVAILABLE`), allowing calling applications to continue operating.

### Trade-offs
- Timeouts must be calibrated based on p99 latency metrics; overly aggressive timeouts cause false-positive failures during transient network spikes.

---

## 5. Reactive Error Handling and Graceful Degradation

<a id="reactive-error-handling-and-graceful-degradation"></a>

### Implementation Comparison

=== "Broken"
    ```java
    --8<-- "modules/21-webclient-webflux/broken-examples/chain-without-error-handling/CartPricingEngine.java"
    ```

=== "Correct"
    ```java
    --8<-- "modules/21-webclient-webflux/src/main/java/lab/webflux/noerrorhandler/CorrectCartPricingEngine.java"
    ```

### Why the Solution Works

1. **Independent Branch Error Isolation**: Applying `.onErrorReturn(0.0)` directly to `loyaltyDiscountMono` and `couponDiscountMono` before passing them to `Mono.zip(...)` guarantees that a failure in one optional service does not abort the other.
2. **Per-Branch Timeout Protection**: Each upstream service has an independent `.timeout(...)` deadline. A hung coupon service does not delay the loyalty discount calculation.
3. **Preserving Core Business Flows**: If both promotional services fail, the customer still receives a valid cart calculation with 0 discount, allowing checkout completion without revenue loss.

### Trade-offs
- Silent fallbacks can mask downstream service outages if not paired with alerts and metrics on fallback execution rates.

---

## Related

- [Concepts](concepts.md) — Fundamental reactive stream principles
- [Code Review](code-review.md) — Review targets and issue analysis
- [Tests](tests.md) — Unit and integration test suites validating these implementations
- [Production](production.md) — Monitoring and diagnostics for reactive services
