# WebClient & WebFlux Code Review

This section presents 5 realistic pull-request code reviews focusing on concurrency hazards, event loop thread stalls, connection pool exhaustion, missing timeouts, and reactive error propagation.

---

## 1. Blocking in Request Flow

### Context
A payment gateway aggregator routes transaction settlement requests to third-party bank providers using Spring WebFlux. An engineer authored `ReactivePaymentGateway.java` to coordinate token verification and payment charging.

### Review Target
```java
--8<-- "modules/21-webclient-webflux/broken-examples/block-in-request-flow/ReactivePaymentGateway.java"
```

### Review Prompt
Review `ReactivePaymentGateway.java` with focus on:
- Synchronous blocking calls (`.block()`, `toFuture().get()`) inside reactive pipelines
- Netty worker event loop thread starvation and throughput collapse
- Preservation of non-blocking reactive chains (`flatMap`, `zipWith`, `then`)
- Backpressure and context propagation across asynchronous boundaries

??? warning "Reveal issues"
    ### Concurrency issue: Calling `.block()` inside WebFlux request flow freezes Netty event loops

    #### Problem
    The method calls `webClient.get().retrieve().bodyToMono(...).block()` directly inside a reactive service method.

    #### Why it happens
    The engineer was familiar with imperative programming and needed the intermediate `TokenValidationResponse` object before executing the subsequent charge request. Calling `.block()` pauses the current execution thread until the HTTP response arrives.

    #### Production impact
    ```text
    8 concurrent token requests hit latency (e.g., 2000ms delay)
    → 8 Netty event loop worker threads blocked on .block()
    → All CPU event loops frozen
    → Gateway stops accepting incoming TCP connections and servicing unrelated routes
    → Health checks fail → Cluster-wide 504 Gateway Timeout
    ```

    #### Why the solution works
    Chaining the operations asynchronously with `.flatMap()` transforms `Mono<TokenValidationResponse>` into `Mono<PaymentResult>` without pausing the thread. When waiting for downstream network packets, the Netty thread is immediately released back to the event loop selector.

    #### Trade-offs
    Requires mastering functional reactive composition and operator chaining rather than standard imperative `if/else` control flow.

    #### How to detect it
    Thread dumps show threads named `reactor-http-epoll-*` in `TIMED_WAITING` or `WAITING` states at `Mono.block()`. BlockHound detects this immediately in CI and throws `BlockingOperationError`.

    #### Interview follow-up
    > What happens if you run `.block()` inside a Spring MVC controller versus a Spring WebFlux controller?

    #### Related
    - [Concepts — Netty Event Loop Architecture](concepts.md#2-netty-event-loop-architecture)
    - [Internals — Netty EventLoop Execution Engine](internals.md#1-netty-eventloop-execution-engine)

[View Solution & Correct Implementation](solutions.md#1-non-blocking-request-flows-avoiding-block)

---

## 2. Blocking JDBC in WebFlux

### Context
An analytics data service exposes customer financial summaries via Spring WebFlux. An engineer wrote `CustomerSummaryService.java` querying a legacy relational database via blocking Spring JDBC.

### Review Target
```java
--8<-- "modules/21-webclient-webflux/broken-examples/blocking-jdbc-in-webflux/CustomerSummaryService.java"
```

### Review Prompt
Review `CustomerSummaryService.java` with focus on:
- Blocking JDBC driver calls executed on the Reactor Netty event loop
- Scheduler isolation (`Schedulers.boundedElastic()`)
- Backpressure, timeout protection, and thread pool bounding

??? warning "Reveal issues"
    ### Concurrency issue: Synchronous blocking JDBC executed on reactive Netty event loop

    #### Problem
    `CustomerSummaryService` wraps `jdbcClient.sql(...).query(...).single()` inside `Mono.fromSupplier(...)` without specifying a scheduler.

    #### Why it happens
    By default, `Mono.fromSupplier` executes on the subscribing thread. In a WebFlux controller, the subscriber thread is the Netty event loop (`reactor-http-epoll-*`). Traditional JDBC drivers use blocking socket reads (`SocketInputStream.read()`) and block on HikariCP connection pool acquisition.

    #### Production impact
    ```text
    10 concurrent database queries hit disk I/O lock
    → All Netty event loops blocked waiting for JDBC socket responses
    → Entire WebFlux instance stops processing non-database HTTP requests
    → Severe latency spikes across all endpoints
    ```

    #### Why the solution works
    Chaining `.subscribeOn(Schedulers.boundedElastic())` moves the execution of the blocking JDBC call to a dedicated worker pool designed for blocking I/O, keeping Netty event loops free.

    #### Trade-offs
    `Schedulers.boundedElastic()` allocates dedicated OS threads. Under heavy database load, thread context switching increases, and HikariCP connection limits must be sized appropriately alongside the scheduler queue.

    #### How to detect it
    BlockHound catches `SocketInputStream.socketRead` on non-blocking threads. Metric `reactor_schedulers_active_threads` tracks scheduler utilization.

    #### Interview follow-up
    > Why not replace JDBC with R2DBC instead of using `boundedElastic()`?

    #### Related
    - [Concepts — Reactor Threading Model and Schedulers](concepts.md#7-reactor-threading-model-and-schedulers)
    - [Concepts — R2DBC vs JDBC: The Persistence Trade-Off](concepts.md#9-r2dbc-vs-jdbc-the-persistence-trade-off)

[View Solution & Correct Implementation](solutions.md#2-offloading-blocking-jdbc-to-boundedelastic)

---

<a id="uncontrolled-flatmap-concurrency"></a>
## 3. Uncontrolled flatMap Concurrency

### Context
A notification aggregation service batches alerts for hundreds of thousands of users. An engineer authored `NotificationBatchSender.java` to dispatch notifications via `Flux.fromIterable(messages).flatMap(...)`.

### Review Target
```java
--8<-- "modules/21-webclient-webflux/broken-examples/uncontrolled-flatmap-concurrency/NotificationBatchSender.java"
```

### Review Prompt
Review `NotificationBatchSender.java` with focus on:
- Default concurrency parameter in `flatMap`
- Outbound socket/connection pool exhaustion (`PoolAcquireTimeoutException`)
- Downstream rate limit triggering (HTTP 429)
- Error isolation across individual message items in the batch

??? warning "Reveal issues"
    ### Scalability issue: Uncontrolled `flatMap` concurrency oversubscribes outbound connection pool

    #### Problem
    `Flux.fromIterable(messages).flatMap(...)` executes with default prefetch and concurrency (`Queues.SMALL_BUFFER_SIZE` = 256).

    #### Why it happens
    The developer assumed `flatMap` processes elements sequentially or with gentle concurrency. For a list of 10,000 notifications, `flatMap` eagerly initiates 256 parallel HTTP requests immediately.

    #### Production impact
    ```text
    3 concurrent batch jobs × 256 in-flight calls = 768 simultaneous HTTP connections
    → Exceeds Reactor Netty default connection pool limit (500 connections)
    → Requests queue in PendingAcquireQueue
    → Pending requests exceed pendingAcquireTimeout (45s)
    → PoolAcquireTimeoutException throws across the entire application
    → Downstream push notification service triggers HTTP 429 rate limit
    ```

    #### Why the solution works
    Supplying an explicit concurrency bound via `flatMap(mapper, maxConcurrency)` (e.g. 16 or 32) restricts the active in-flight requests, preventing connection pool exhaustion and downstream saturation.

    #### Trade-offs
    Lower concurrency limits increase total batch wall-clock time in exchange for rock-solid stability and predictable connection pool utilization.

    #### How to detect it
    Logs show `io.netty.channel.ConnectTimeoutException` and `reactor.netty.internal.shaded.reactor.pool.PoolAcquireTimeoutException`.

    #### Interview follow-up
    > How does `concatMap` differ from `flatMap` with concurrency=1?

    #### Related
    - [Internals — Reactor Netty Connection Pool State Machine](internals.md#4-reactor-netty-connection-pool-state-machine)
    - [Questions — Q12 FlatMap Concurrency Limiting](questions.md#q12-how-does-fluxflatmapmapper-maxconcurrency-enforce-concurrency-limits-compared-to-default-unbound-concurrency)

[View Solution & Correct Implementation](solutions.md#3-bounded-flatmap-concurrency-and-error-isolation)

---

## 4. Missing WebClient Timeout

### Context
A logistics shipment tracker queries an external carrier API to compute delivery ETAs. An engineer authored `CarrierTrackingClient.java` configuring `WebClient` using standard default builders.

### Review Target
```java
--8<-- "modules/21-webclient-webflux/broken-examples/missing-webclient-timeout/CarrierTrackingClient.java"
```

### Review Prompt
Review `CarrierTrackingClient.java` with focus on:
- TCP connect timeouts on the underlying Netty `HttpClient`
- Response and read timeouts on the reactive channel
- Operator-level `.timeout()` boundaries
- Graceful degradation when downstream carrier is unreachable

??? warning "Reveal issues"
    ### Resilience issue: Missing response timeout on WebClient Netty connector

    #### Problem
    `WebClient.builder().baseUrl(...).build()` creates a `WebClient` backed by default Reactor Netty `HttpClient` without custom timeouts.

    #### Why it happens
    The developer assumed Spring Boot automatically provides aggressive default timeouts. In reality, default Reactor Netty `HttpClient` instances have an **unbounded response timeout** and a 30-second connection timeout.

    #### Production impact
    ```text
    Carrier service stalls on TLS handshake or leaves TCP socket open without transmitting bytes
    → Calling WebClient holds connection slot indefinitely
    → Connection pool channels remain permanently leased
    → Pool exhausts → Subsequent requests block and time out
    ```

    #### Why the solution works
    Configuring `HttpClient.create().responseTimeout(Duration.ofSeconds(3))` and channel option `CONNECT_TIMEOUT_MILLIS` guarantees that half-open TCP connections and unresponsive endpoints are severed quickly. Adding a stream-level `.timeout(Duration)` with `.onErrorResume()` returns graceful fallback tracking info.

    #### Trade-offs
    Tight timeouts cause premature failures on slow mobile networks unless balanced with jittered retries.

    #### How to detect it
    Metric `reactor.netty.connection.provider.active.connections` remains at maximum capacity while traffic drops to near zero.

    #### Interview follow-up
    > What is the difference between a Netty channel response timeout and an operator-level `.timeout(Duration)`?

    #### Related
    - [Concepts — Critical WebClient Invariants](concepts.md#critical-webclient-invariants)
    - [Questions — Q08 WebClient Timeout Basics](questions.md#q08-how-do-you-configure-basic-connect-and-response-timeouts-on-webclient)

[View Solution & Correct Implementation](solutions.md#4-configuring-webclient-timeouts)

---

<a id="chain-without-error-handling"></a>
## 5. Chain Without Error Handling

### Context
A pricing discount engine queries multiple upstream catalog and promotional services to calculate the best available discount for a customer cart. An engineer authored `CartPricingEngine.java` combining reactive streams with `Mono.zip`.

### Review Target
```java
--8<-- "modules/21-webclient-webflux/broken-examples/chain-without-error-handling/CartPricingEngine.java"
```

### Review Prompt
Review `CartPricingEngine.java` with focus on:
- Unhandled error events in reactive pipelines
- Terminal nature of `onError` in Reactive Streams specification
- Graceful degradation via `.onErrorReturn()` or `.onErrorResume()`
- Independent failure isolation in parallel multi-source aggregation

??? warning "Reveal issues"
    ### Resilience issue: Terminal `onError` in `Mono.zip` collapses entire checkout calculation

    #### Problem
    `CartPricingEngine` combines `loyaltyDiscountMono` and `couponDiscountMono` using `Mono.zip(...)` without decorating either branch with error handling or timeouts.

    #### Why it happens
    The engineer assumed that if one optional discount service failed, `Mono.zip` would provide a null value or partial tuple. However, in Reactive Streams, an `onError` signal is terminal.

    #### Production impact
    ```text
    Coupon service encounters a transient 503 error
    → couponDiscountMono emits onError
    → Mono.zip immediately aborts and cancels loyaltyDiscountMono
    → calculateCartPrice emits HTTP 500
    → Checkout page crashes, preventing customer from purchasing items
    ```

    #### Why the solution works
    Applying `.onErrorReturn(0.0)` and `.timeout(...)` directly to each independent discount branch before passing them to `Mono.zip` isolates failures. If the coupon service fails or times out, the cart calculation proceeds with a default 0.0 discount.

    #### Trade-offs
    Returning fallback defaults (0.0 discount) hides downstream microservice outages from end users, so business dashboards must monitor fallback activation rates via metrics.

    #### How to detect it
    Log aggregators show unhandled `WebClientResponseException$InternalServerError` bubbling out of `calculateCartPrice`.

    #### Interview follow-up
    > Why does placing `.onErrorReturn()` after `Mono.zip(...)` fail to preserve the loyalty discount when the coupon service errors?

    #### Related
    - [Concepts — Project Reactor: Mono and Flux](concepts.md#4-project-reactor-mono-and-flux)
    - [Questions — Q13 Reactive Error Handling Strategies](questions.md#q13-what-is-the-difference-between-onerrorresume-onerrorreturn-and-onerrormap)

[View Solution & Correct Implementation](solutions.md#5-reactive-error-handling-and-graceful-degradation)

---

## Related

- [Solutions](solutions.md) — Detailed code diffs and architectural fixes
- [Tests](tests.md) — Validating reactive behavior and error isolation with StepVerifier
- [Questions](questions.md) — Comprehensive interview questions on reactive design
- [Production](production.md) — Real-world outage triage and monitoring
