# WebClient & WebFlux Interview Questions

<!-- --8<-- [start:basic] -->
## Basic Questions (8)

### Q01: What is the fundamental difference between Spring MVC and Spring WebFlux?

??? question "Reveal answer"
    **Short Answer:**
    Spring MVC uses a synchronous, thread-per-request model (typically on Apache Tomcat) where each active request occupies a dedicated operating system thread that blocks during I/O. Spring WebFlux uses an asynchronous, non-blocking event loop model (typically on Netty) where a small pool of worker threads (1 per CPU core) multiplexes thousands of concurrent connections using non-blocking OS selectors (`epoll`/`kqueue`).

    ??? example "Example"
        ```java
        --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q01BlockingVsNonBlockingEventLoopExample.java"
        ```

---

### Q02: What are the four core interfaces in the Reactive Streams specification?

??? question "Reveal answer"
    **Short Answer:**
    The Reactive Streams standard defines:
    1. `Publisher<T>`: Emits elements asynchronously.
    2. `Subscriber<T>`: Receives items and terminal signals (`onSubscribe`, `onNext`, `onError`, `onComplete`).
    3. `Subscription`: Manages the link between Publisher and Subscriber, enabling backpressure via `request(n)` and cancellation via `cancel()`.
    4. `Processor<T, R>`: Represents a processing stage acting as both a Subscriber and Publisher.

    ??? example "Example"
        ```java
        --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q02ReactiveStreamsInterfacesExample.java"
        ```

---

### Q03: What is the semantic difference between `Mono<T>` and `Flux<T>` in Project Reactor?

??? question "Reveal answer"
    **Short Answer:**
    `Mono<T>` represents an asynchronous sequence that emits at most one value (0 or 1 item) followed by a terminal completion or error signal, analogous to an asynchronous `Optional` or `CompletableFuture`. `Flux<T>` represents an asynchronous sequence that emits 0 to N values, analogous to an asynchronous `Iterable` or Java `Stream`.

    ??? example "Example"
        ```java
        --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q03MonoVsFluxSemanticsExample.java"
        ```

---

### Q04: Why does a reactive pipeline not execute any logic until a subscriber attaches?

??? question "Reveal answer"
    **Short Answer:**
    Reactor publishers are "cold" by default: declaring a pipeline (`Mono.fromSupplier(...)` or `flux.map(...)`) merely builds an in-memory assembly graph of publisher decorators. No computation, network I/O, or database queries run until a subscriber explicitly invokes `.subscribe()`, at which point the subscription signal propagates upstream to trigger data generation ("nothing happens until you subscribe").

    ??? example "Example"
        ```java
        --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q04LazySubscriptionEvaluationExample.java"
        ```

---

### Q05: What is the difference between `.map()` and `.flatMap()` in Project Reactor?

??? question "Reveal answer"
    **Short Answer:**
    `.map(Function<T, R>)` transforms an element synchronously 1-to-1 (`T -> R`) on the current thread and wraps the returned value in a Publisher. `.flatMap(Function<T, Publisher<V>>)` transforms an element into an asynchronous inner Publisher (`T -> Mono<V>` or `T -> Flux<V>`), subscribes to that inner Publisher, and merges the resulting emissions into a flattened outer stream.

    ??? example "Example"
        ```java
        --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q05MapVsFlatMapOperatorsExample.java"
        ```

---

### Q06: How does `.subscribeOn()` differ from `.publishOn()` in a Reactor pipeline?

??? question "Reveal answer"
    **Short Answer:**
    `subscribeOn(Scheduler)` influences the thread that executes the initial subscription and source generation, propagating upstream to the root Publisher regardless of where it is declared. `publishOn(Scheduler)` influences only downstream operators following the declaration by enqueuing items and switching execution to the specified scheduler.

    ??? example "Example"
        ```java
        --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q06SubscribeOnVsPublishOnExample.java"
        ```

---

### Q07: What is backpressure and how does the `Subscription.request(n)` contract prevent consumer overflow?

??? question "Reveal answer"
    **Short Answer:**
    Backpressure is a flow-control mechanism where a downstream subscriber signals its current processing capacity to an upstream publisher using `Subscription.request(n)`. The publisher is strictly forbidden from emitting more than $n$ elements until the subscriber issues another `request(m)` call, preventing fast producers from exhausting consumer heap buffers.

    ??? example "Example"
        ```java
        --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q07BackpressureRequestNExample.java"
        ```

---

### Q08: How do you configure basic connect and response timeouts on `WebClient`?

??? question "Reveal answer"
    **Short Answer:**
    Because default `WebClient.builder()` instances lack response timeouts, production configurations must customize the underlying Reactor Netty `HttpClient`: use `ChannelOption.CONNECT_TIMEOUT_MILLIS` on the TCP channel builder for connection deadlines and `HttpClient.responseTimeout(Duration)` for HTTP response body deadlines.

    ??? example "Example"
        ```java
        --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q08WebClientTimeoutBasicsExample.java"
        ```
<!-- --8<-- [end:basic] -->

---

<!-- --8<-- [start:intermediate] -->
## Intermediate Questions (8)

### Q09: How does Reactor Netty's connection pool manage outbound TCP channels, and what causes `PoolAcquireTimeoutException`?

??? question "Reveal answer"
    **Short Answer:**
    Reactor Netty's `ConnectionProvider` allocates a bounded pool of TCP channels per remote host (default 500 connections). When all connections are leased and new requests arrive, requests enter a pending acquire queue. If a request waits in the pending queue longer than `pendingAcquireTimeout` (default 45s) without acquiring an idle channel, Reactor Netty throws `PoolAcquireTimeoutException`.

    **Internal Mechanism:**
    Under the hood, `SimpleConnectionProvider` maintains a `ConcurrentLinkedDeque` of idle channels and an `AbstractQueue` of pending subscriber promises. Each time a channel completes its HTTP response body streaming and remains keep-alive, it is returned to the pool and immediately fulfills the oldest pending promise. If downstream response latency spikes or parallel requests explode, the pending queue saturates.

    **Common Mistake:**
    Treating `PoolAcquireTimeoutException` as a downstream server connection error and arbitrarily increasing the pool size to 5,000, which causes socket exhaustion and memory starvation on the host OS.

    ??? example "Example"
        ```java
        --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q09ReactorNettyConnectionPoolAcquireExample.java"
        ```

---

### Q10: What are the differences between Reactor `Schedulers.parallel()`, `Schedulers.boundedElastic()`, and `Schedulers.single()`?

??? question "Reveal answer"
    **Short Answer:**
    `Schedulers.parallel()` is a fixed-size pool matching CPU core count, optimized for non-blocking computational tasks. `Schedulers.boundedElastic()` is a dynamically sized pool (up to $10 \times \text{CPU cores}$, max 100k queue) designed specifically to isolate blocking I/O (JDBC, legacy REST clients) from event loops. `Schedulers.single()` is a dedicated single background thread for serialized execution.

    **Internal Mechanism:**
    `parallel()` worker threads implement `NonBlockingMarker`, causing BlockHound to reject any blocking calls. `boundedElastic()` uses worker leasing with TTL eviction (60s) and does not implement the marker, allowing thread blocking while maintaining an upper bound to avoid unbounded thread creation.

    **Common Mistake:**
    Using `Schedulers.elastic()` (which was deprecated because it allows unbounded thread creation leading to JVM OOM) or executing blocking JDBC inside `Schedulers.parallel()`, which starves CPU computation.

    ??? example "Example"
        ```java
        --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q10ReactorSchedulersComparisonExample.java"
        ```

---

### Q11: How does BlockHound detect blocking calls inside Netty event loops?

??? question "Reveal answer"
    **Short Answer:**
    BlockHound installs a ByteBuddy instrumentation agent at JVM startup that rewrites bytecode for known blocking JDK methods (`Thread.sleep`, `SocketInputStream.read`, `FileInputStream.read`). When invoked, the instrumented method checks if `Thread.currentThread() instanceof NonBlocking`. If true, it immediately throws `BlockingOperationError`.

    **Internal Mechanism:**
    Reactor Netty event loop threads and `Schedulers.parallel()` threads implement the `reactor.core.scheduler.NonBlocking` interface. BlockHound intercepts blocking entrypoints at the native/JVM boundary, inspecting the caller thread type before the OS system call executes.

    **Common Mistake:**
    Relying solely on code reviews to catch blocking calls. Third-party libraries, JSON loggers, or `UUID.randomUUID()` (which reads `/dev/random` on older Linux kernels) often hide blocking I/O deep in call stacks.

    ??? example "Example"
        ```java
        --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q11BlockHoundInstrumentationExample.java"
        ```

---

### Q12: How does `Flux.flatMap(mapper, maxConcurrency)` enforce concurrency limits compared to default unbound concurrency?

??? question "Reveal answer"
    **Short Answer:**
    Default `Flux.flatMap(fn)` uses an internal prefetch and concurrency limit of `Queues.SMALL_BUFFER_SIZE` (256 concurrent subscribers). For large streams, it eagerly subscribes to up to 256 inner publishers simultaneously. Supplying `flatMap(fn, maxConcurrency)` restricts the number of active in-flight inner subscriptions, pulling new items from upstream only as previous inner publishers complete.

    **Internal Mechanism:**
    `FlatMapSubscriber` tracks active inner subscribers via an atomic counter. When an item arrives from upstream, if `activeCount < maxConcurrency`, it subscribes to the inner publisher; otherwise, it stops requesting items from upstream (`Subscription.request()`), applying backpressure until an inner publisher emits `onComplete`.

    **Common Mistake:**
    Processing a batch of 10,000 items with `Flux.fromIterable(batch).flatMap(api::call)` without specifying concurrency, overwhelming downstream APIs with hundreds of simultaneous HTTP requests.

    ??? example "Example"
        ```java
        --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q12FlatMapConcurrencyLimitingExample.java"
        ```

---

### Q13: What is the difference between `.onErrorResume()`, `.onErrorReturn()`, and `.onErrorMap()`?

??? question "Reveal answer"
    **Short Answer:**
    In Reactive Streams, an `onError` signal is terminal and tears down the stream. `.onErrorReturn(value)` intercepts the error and emits a fallback default value before completing. `.onErrorResume(fn)` catches the error and switches execution to an alternative fallback Publisher. `.onErrorMap(fn)` intercepts low-level exceptions and translates them into domain exceptions without stopping the error propagation.

    **Internal Mechanism:**
    Error operators wrap downstream subscribers with specialized error interceptors. When upstream invokes `onError(t)`, the operator intercepts the signal instead of forwarding it, invoking the fallback logic (`onNext(fallback) -> onComplete()` for `onErrorReturn`, or subscribing to the fallback publisher for `onErrorResume`).

    **Common Mistake:**
    Placing error handlers *after* operators like `Mono.zip()` instead of on the individual inner branches. If an inner branch fails unhandled, `Mono.zip` terminates immediately, discarding the other parallel results.

    ??? example "Example"
        ```java
        --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q13ReactiveErrorHandlingStrategiesExample.java"
        ```

---

### Q14: How does R2DBC provide non-blocking database access, and what are its trade-offs compared to JDBC/JPA?

??? question "Reveal answer"
    **Short Answer:**
    R2DBC implements fully asynchronous database wire protocols over Netty socket channels, returning `Mono` and `Flux` query results without blocking worker threads. The trade-off is the loss of JPA/Hibernate object-relational mapping (lazy loading, dirty checking, entity graphs, second-level caches) and a smaller driver ecosystem.

    **Internal Mechanism:**
    R2DBC drivers encode database protocol frames (such as PostgreSQL frontend/backend messages) onto Netty `ChannelHandlerContext`. Queries are serialized as asynchronous socket writes, and row packets are decoded into streaming reactive publishers.

    **Common Mistake:**
    Attempting to use Hibernate / Spring Data JPA within a WebFlux application without offloading to `Schedulers.boundedElastic()`, which blocks the Netty event loop on every query.

    ??? example "Example"
        ```java
        --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q14R2dbcVsJdbcNonBlockingExample.java"
        ```

---

### Q15: How does Project Reactor's `ContextView` propagate request context across thread boundaries without `ThreadLocal`?

??? question "Reveal answer"
    **Short Answer:**
    Reactor `Context` is an immutable key-value store attached to the `Subscriber` at the bottom of the execution pipeline via `.contextWrite()`. It flows upstream from the subscriber to all operators during the subscription phase, allowing operators to read contextual data (trace IDs, security tokens) regardless of which thread pool executes the operator.

    **Internal Mechanism:**
    During assembly, `contextWrite` decorates the subscriber chain. During subscription, each operator queries `downstreamSubscriber.currentContext()`. Because it travels along the subscriber object hierarchy rather than OS thread local storage, context survives thread switching between Netty event loops and Schedulers.

    **Common Mistake:**
    Writing to `Context` inside `flatMap` expecting downstream operators to see it. Reactor Context flows **upstream** from subscription to source, not downstream from emission to subscriber.

    ??? example "Example"
        ```java
        --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q15ReactorContextPropagationExample.java"
        ```

---

### Q16: How do retry strategies with exponential backoff and jitter work in WebClient via `Retry.backoff()`?

??? question "Reveal answer"
    **Short Answer:**
    `Retry.backoff(maxAttempts, minBackoff)` recalculates delays exponentially for each retry ($d = \text{minBackoff} \times 2^{\text{attempt}-1}$). It applies randomized jitter to prevent all retrying clients from hammering a recovering downstream service in synchronized bursts (the Thundering Herd problem).

    **Internal Mechanism:**
    Under the hood, `RetryBackoffSpec` resubscribes to the upstream publisher via a companion `Flux` triggered by `Mono.delay(calculatedDelay, Schedulers.parallel())`. The jitter factor introduces a uniform random multiplier ($\pm 50\%$ by default) to decorrelate concurrent retries.

    **Common Mistake:**
    Applying retries indiscriminately to non-idempotent HTTP POST requests or retrying client 4xx errors (e.g. 400 Bad Request, 404 Not Found), which can never succeed on retry.

    ??? example "Example"
        ```java
        --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q16WebClientExponentialBackoffRetryExample.java"
        ```
<!-- --8<-- [end:intermediate] -->

---

<!-- --8<-- [start:senior] -->
## Senior Questions (5)

### Q17: In Java 21+ with Virtual Threads available in Spring Boot, when should an engineering team choose Spring WebFlux over Spring MVC with Virtual Threads, and when should WebFlux be avoided?

??? question "Reveal answer"
    **Short Answer:**
    Spring MVC with Virtual Threads is the superior choice for typical CRUD, relational database, and microservice workloads because it allows writing clean imperative code using mature JPA/Hibernate tooling without event loop starvation risks. Spring WebFlux remains essential for high-concurrency network proxies, Server-Sent Events (SSE), WebSockets, and fine-grained reactive stream transformations (`combineLatest`, `sample`, `window`). WebFlux should be avoided when applications are bound to blocking relational databases or when team cognitive overhead with reactive pipelines leads to subtle blocking bugs.

    **Deep Explanation:**
    Java 21 Virtual Threads decouple thread allocation from OS thread limits by parking virtual threads on carrier threads during blocking socket I/O. This eliminates the scalability bottleneck of Spring MVC without requiring reactive stream APIs. However, Virtual Threads do not provide reactive streaming semantics: they cannot perform true non-blocking backpressure, client-driven windowing, or streaming event multiplexing across WebSockets. WebFlux excels in edge gateways (e.g. Spring Cloud Gateway) where thousands of idle client sockets are multiplexed over minimal system resources.

    **Internal Mechanism:**
    Virtual Threads park via JVM continuation mechanics when encountering blocking socket reads. WebFlux relies on Netty `ChannelHandler` pipelines and OS kernel readiness notifications (`epoll_wait`). Virtual Threads still allocate JVM objects for thread state, whereas Netty event loops reuse pooled buffers and zero-copy slicing.

    **Example:**
    ```java
    --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q17WebFluxVsVirtualThreadsTradeoffsExample.java"
    ```

    **Common Mistake:**
    Migrating a standard CRUD Spring Boot application to Spring WebFlux thinking it will make database queries "faster", only to encounter blocking JDBC issues or complex reactive transaction bugs.

    **Production Consideration:**
    Virtual Threads can experience pinning if code synchronizes on `synchronized` blocks holding I/O (addressed in Java 24), whereas WebFlux requires strict adherence to non-blocking discipline verified by BlockHound.

    **Follow-up Questions:**
    1. How do you monitor carrier thread saturation when using Virtual Threads in Spring Boot?
    2. Can you use `WebClient` inside a Spring MVC application running on Virtual Threads? (Yes, `webClient.get().retrieve().bodyToMono(...).block()` is safe on virtual threads because blocking parks the virtual thread without blocking the OS carrier thread).

---

### Q18: How do you design and test backpressure handling in high-throughput streaming pipelines using `onBackpressureBuffer`, `onBackpressureDrop`, and `onBackpressureLatest`?

??? question "Reveal answer"
    **Short Answer:**
    Backpressure strategies decouple upstream emission speed from downstream consumption capacity. Buffer strategies absorb transient bursts up to a fixed bound; drop strategies discard excess elements to protect memory; latest strategies preserve the most recent telemetry state. In tests, `TestPublisher` and `StepVerifier.create(flux, initialRequest)` verify that publishers strictly adhere to downstream demand limits.

    **Deep Explanation:**
    When upstream produces faster than downstream can process (e.g., high-frequency IoT sensors or market pricing tickers), unbounded buffering results in JVM heap exhaustion. Choosing a strategy depends on data semantics:
    - Financial orders require bounded buffering with backpressure rejection (`BufferOverflowException`).
    - Metric telemetry benefits from `onBackpressureDrop` with a counter metric tracking dropped packets.
    - Real-time vehicle GPS coordinates benefit from `onBackpressureLatest` because older unconsumed positions are obsolete.

    **Internal Mechanism:**
    `onBackpressureBuffer` maintains an internal `MpscLinkedQueue`. When queue size exceeds capacity, it invokes the configured `BufferOverflowStrategy` (`DROP_OLDEST`, `DROP_LATEST`, or `ERROR`). `onBackpressureDrop` checks `requested > 0`; if zero, it immediately invokes the drop callback and discards the reference.

    **Example:**
    ```java
    --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q18BackpressureBufferDropLatestStrategiesExample.java"
    ```

    **Common Mistake:**
    Using unbounded `onBackpressureBuffer()` without a capacity limit, which merely delays an eventual `OutOfMemoryError` during sustained consumer degradation.

    **Production Consideration:**
    Always attach an alertable Micrometer metric to `onBackpressureDrop` callbacks so operational teams detect consumer saturation before clients report missing events.

    **Follow-up Questions:**
    1. How does `StepVerifier.withVirtualTime` simulate hours of slow stream processing in milliseconds?
    2. How does HTTP chunked transfer encoding propagate backpressure across network boundaries in WebFlux?

---

### Q19: How do you diagnose and debug memory leaks in Netty ByteBuf allocations and leaked Reactor operators?

??? question "Reveal answer"
    **Short Answer:**
    Netty ByteBuf leaks occur when pooled direct buffers are retained without decrementing reference counts (`buf.release()`). Leaks are diagnosed using Netty's `ResourceLeakDetector` (`-Dio.netty.leakDetection.level=ADVANCED`). Reactor operator assembly debugging in production is enabled safely using the Reactor Debug Agent (ByteBuddy load-time transformation) rather than `Hooks.onOperatorDebug()`, which introduces severe runtime stack trace generation overhead.

    **Deep Explanation:**
    Reactor Netty uses pooled off-heap `ByteBuf` instances to avoid garbage collection churn during high-volume network streaming. Each buffer maintains a reference count. If an operator or custom channel handler discards a buffer without consuming or releasing it, native memory is leaked. In addition, subscribing to infinite reactive streams without storing disposable handles or cancellation listeners retains references in memory indefinitely.

    **Internal Mechanism:**
    `ResourceLeakDetector` samples `ByteBuf` allocations (1% of buffers at `SIMPLE`, 100% at `PARANOID`) and wraps them in phantom references. When the JVM GC collects the Java wrapper without `release()` having been called, Netty logs an error containing the allocation stack trace.

    **Example:**
    ```java
    --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q19ByteBufLeakDetectionAndOperatorDebugExample.java"
    ```

    **Common Mistake:**
    Enabling `Hooks.onOperatorDebug()` in production environments, which forces every reactive operator instantiation to capture a snapshot of the execution thread stack trace, degrading throughput by 50% or more.

    **Production Consideration:**
    In Kubernetes, native memory leaks manifest as container OOMKills (`ExitCode 137`) where JVM heap utilization remains low and stable while OS resident set size (RSS) continuously climbs.

    **Follow-up Questions:**
    1. What is the difference between Netty heap buffers and direct off-heap buffers?
    2. How does Micrometer Observation provide distributed trace context without the performance cost of operator debugging?

---

### Q20: How do you implement robust circuit breaking, fallback, and bulkhead isolation in a WebClient microservice mesh with Resilience4j?

??? question "Reveal answer"
    **Short Answer:**
    Resilience4j provides reactive operators (`CircuitBreakerOperator`, `BulkheadOperator`, `TimeLimiterOperator`) that decorate Reactor `Mono` and `Flux` publishers directly. Circuit breakers fail fast when failure or slow-call rates cross thresholds; bulkheads restrict concurrent active subscribers to prevent connection pool exhaustion; and time limiters enforce deadlines with graceful fallback values.

    **Deep Explanation:**
    In a microservice architecture, cascading failures occur when an unresponsive downstream service ties up connection pool slots and memory across calling services. Applying Resilience4j reactive decorators ensures that calls failing repeatedly trip the circuit breaker into `OPEN` state, immediately executing `.onErrorResume()` fallbacks without initiating network calls. Bulkhead operators limit concurrent executions to a dedicated quota, protecting the shared Netty client pool.

    **Internal Mechanism:**
    Resilience4j's `CircuitBreaker` maintains a ring-bit-buffer sliding window of call outcomes. When `transform(CircuitBreakerOperator.of(cb))` is chained, the operator intercepts `onNext`, `onError`, and cancellation signals, recording execution latency and state transitions in lock-free atomic registers.

    **Example:**
    ```java
    --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q20WebClientCircuitBreakerBulkheadExample.java"
    ```

    **Common Mistake:**
    Applying traditional synchronous Resilience4j decorators (`circuitBreaker.executeSupplier(...)`) around reactive methods, which executes the decorator at *assembly time* rather than *execution time*.

    **Production Consideration:**
    Configure distinct sliding window sizes and minimum call thresholds for each external client. Expose circuit breaker state changes via Micrometer metrics to trigger automated alerts when circuits open.

    **Follow-up Questions:**
    1. Why should a Semaphore Bulkhead be preferred over a ThreadPool Bulkhead in a reactive WebFlux application? (Because reactive code already runs asynchronously on event loops; thread pool bulkheads introduce unnecessary context switching).
    2. How does Resilience4j handle half-open state transitions under reactive traffic?

---

### Q21: How do you handle distributed transaction boundaries and data consistency across microservices in a fully reactive WebFlux architecture?

??? question "Reveal answer"
    **Short Answer:**
    In a reactive microservice architecture, traditional Two-Phase Commit (2PC) or distributed XA transactions are anti-patterns that destroy non-blocking scalability. Distributed consistency must be achieved via the asynchronous Saga pattern (orchestrated or choreographed) combining non-blocking WebClient calls, reactive database operations (R2DBC), and idempotent compensating transactions.

    **Deep Explanation:**
    Because R2DBC transactions are scoped to local database connection publishers via Reactor Context, transactions cannot span network calls. An orchestrated Saga coordinates steps by chaining reactive operators (`flatMap`). If a step fails, the coordinator triggers compensating reactive publishers (`cancelOrder`, `refundPayment`). All compensations must be strictly idempotent to handle network retries safely.

    **Internal Mechanism:**
    Spring's `ReactiveTransactionManager` (e.g. `R2dbcTransactionManager`) binds the active database connection to the subscriber's Reactor `Context`. When calling external microservices via WebClient inside a reactive flow, the external HTTP call occurs outside the database transaction boundary, preserving short database lock durations.

    **Example:**
    ```java
    --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q21ReactiveDistributedTransactionsSagaExample.java"
    ```

    **Common Mistake:**
    Attempting to hold a reactive database transaction open across an external WebClient network call, holding database connection slots during remote network latency.

    **Production Consideration:**
    Combine reactive Sagas with the Transactional Outbox pattern implemented via R2DBC to guarantee that domain events are published reliably even if the application crashes mid-orchestration.

    **Follow-up Questions:**
    1. How does the Inbox Pattern prevent duplicate event processing in a reactive consumer?
    2. What is the role of a pivot transaction in a reactive Saga workflow?
<!-- --8<-- [end:senior] -->

---

<!-- --8<-- [start:scenarios] -->
## Scenario Questions (2)

### Q22: Incident: High latency in a downstream authentication service freezes all unrelated routes on a Spring WebFlux gateway. How do you triage, identify the root cause, and remediate?

??? question "Reveal answer"
    **Short Answer:**
    The root cause is an accidental blocking call (`.block()`, synchronous LDAP, or synchronous JDBC) executing directly on the Netty event loop thread. Because the gateway runs only 1 event loop per CPU core (e.g. 8 threads), 8 concurrent slow authentication calls freeze all 8 event loops, preventing Netty from servicing OS socket readiness events for any route. Remediate by offloading blocking calls to `Schedulers.boundedElastic()`, adding request timeouts, and enforcing BlockHound in CI/CD.

    **Deep Explanation:**
    In Spring WebFlux, all HTTP routes share the same `EventLoopGroup`. When downstream auth latency spikes from 10ms to 10 seconds, threads executing synchronous blocking calls pause in OS wait states. Netty cannot execute `selector.select()` or drain scheduled tasks. Health check endpoints (`/actuator/health`) and unrelated APIs stop responding, resulting in 504 Gateway Timeouts across the entire cluster.

    **Internal Mechanism:**
    Taking a thread dump reveals all `reactor-http-epoll-*` threads in `WAITING` or `TIMED_WAITING` states inside `Mono.block()` or a socket read method rather than cycling in `SingleThreadEventLoop.run()`.

    **Example:**
    ```java
    --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q22IncidentEventLoopBlockingExhaustionExample.java"
    ```

    **Common Mistake:**
    Attempting to fix the outage by scaling out CPU limits or increasing the number of gateway pods, which only delays the freeze until concurrent traffic fills the new pods' event loops.

    **Production Consideration:**
    Install BlockHound in integration test suites to catch blocking calls at build time. Implement route-level timeouts and fallback responses on the authentication gateway filter.

    **Follow-up Questions:**
    1. What metrics in Micrometer alert on Netty event loop queue latency?
    2. How does an async non-blocking reactive cache (e.g. Caffeine async or Redis reactive) prevent authentication service stampedes?

---

### Q23: Incident: A batch payment notification service encounters massive `PoolAcquireTimeoutException` errors and downstream 429 throttling under peak load. How do you identify the defect and redesign the pipeline?

??? question "Reveal answer"
    **Short Answer:**
    The defect is an unconstrained `Flux.flatMap` operator processing thousands of items without a concurrency limit (`flatMap(fn, maxConcurrency)`). Default `flatMap` eagerly initiates 256 parallel inner subscriptions per batch, quickly exhausting Reactor Netty's default 500-channel connection pool and saturating downstream rate limits. Redesign by enforcing an explicit concurrency limit (e.g. `flatMap(fn, 16)`), adding inner stream error isolation, and configuring exponential backoff retries.

    **Deep Explanation:**
    When batch jobs submit 10,000 notifications via `Flux.fromIterable(batch).flatMap(client::sendNotification)`, flatMap immediately requests 256 elements from the iterator. With multiple concurrent batches, hundreds of HTTP requests compete for connection pool leases. Requests waiting longer than `pendingAcquireTimeout` (45s) throw `PoolAcquireTimeoutException`. Furthermore, downstream services respond with HTTP 429 (Too Many Requests), which aborts the entire batch because inner errors are not isolated.

    **Internal Mechanism:**
    Reactor Netty's `PendingConnectionAllocations` queue hits its ceiling. Channel leases exceed `maxConnections`, and unhandled `onError` signals bubble up to terminate the outer `Flux`, cancelling all remaining in-flight and pending messages in the batch.

    **Example:**
    ```java
    --8<-- "modules/21-webclient-webflux/src/examples/java/lab/webflux/questions/Q23IncidentFlatMapPoolAcquireTimeoutExample.java"
    ```

    **Common Mistake:**
    Increasing `maxConnections` to 10,000 to "fix" the pool acquire timeout, which overwhelms the downstream service, burns through network sockets, and triggers severe firewall throttling.

    **Production Consideration:**
    Pair bounded `flatMap(fn, concurrency)` with rate limiting (`Flux.delayElements`) and isolate errors on the inner publisher (`.onErrorResume(...)`) so individual failed notifications are routed to a Dead Letter Queue (DLQ) without aborting the batch.

    **Follow-up Questions:**
    1. How does `concatMap` differ from `flatMap` with concurrency=1?
    2. When should you choose `flatMapSequential` instead of `flatMap`?
<!-- --8<-- [end:scenarios] -->

---

## Related

- [Concepts](concepts.md) — Fundamental reactive streams and WebClient mechanics
- [Internals](internals.md) — Deep dive into Netty event loops and Reactor schedulers
- [Code Review](code-review.md) — Spotting and fixing real-world WebFlux anti-patterns
- [Solutions](solutions.md) — Production code patterns and trade-off evaluations
