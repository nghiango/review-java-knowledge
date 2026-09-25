# Performance Issues

Latency, allocation, resource leaks, saturation and scalability failures.

## Entries

### Reader leak exhausts file descriptors

**Type:** Resource leak issue · **Severity:** High · **Difficulty:** Basic

A method that creates a reader owns its closure. Without try-with-resources, repeated imports retain
OS descriptors on success and failure until the process can no longer open files.

**Detection:** Process open-file metrics, `lsof -p <pid>` and failure-path tests with a close-tracking reader.

**Appears in:** [Core Java — resource and collection mutation](../topics/core-java/code-review.md#resource-and-collection-mutation)

### Blocking I/O in the common ForkJoinPool

**Type:** Performance issue · **Severity:** High · **Difficulty:** Senior

Parallel streams share a process-wide pool tuned for CPU work. Blocking price calls can starve
unrelated streams and CompletableFuture stages. Prefer sequential work or an explicit bounded,
monitored executor.

**Appears in:** [Core Java — stream side effects](../topics/core-java/code-review.md#stream-and-parallel-side-effects)

### Static listener retention

**Type:** Memory issue · **Severity:** High · **Difficulty:** Intermediate

A static listener list keeps strong references to listeners and everything they capture for as long
as the defining class loader remains reachable. Prefer an instance-owned registry whose lifetime is
bound to the component that owns it.

**Appears in:** `modules/02-jvm/broken-examples/static-listener-leak`

### Missing registration lifecycle

**Type:** Resource leak issue · **Severity:** High · **Difficulty:** Intermediate

Registering a listener without returning a closeable registration handle makes cleanup depend on
out-of-band knowledge. Return an explicit handle and close it during component shutdown.

**Appears in:** `modules/02-jvm/broken-examples/static-listener-leak`

### ThreadLocal value retained on pooled thread

**Type:** Resource leak issue · **Severity:** High · **Difficulty:** Intermediate

ThreadLocal values attached to executor threads can outlive the logical request that created them.
Open a closeable scope at the request boundary and remove the value when the scope closes.

**Appears in:** `modules/02-jvm/broken-examples/threadlocal-pool-leak`

### Unbounded static cache

**Type:** Memory issue · **Severity:** High · **Difficulty:** Intermediate

A cache backed by a static map with no maximum size, expiry or weight limit can retain data until
the process fails. Set an explicit bound and eviction policy, and size it with production data.

**Appears in:** `modules/02-jvm/broken-examples/unbounded-cache`

### Caller-controlled cache key cardinality

**Type:** Scalability issue · **Severity:** High · **Difficulty:** Intermediate

Cache keys built from user-controlled request data can grow with traffic cardinality while producing
a low hit rate. Use a bounded key domain and cap retained entries.

**Appears in:** `modules/02-jvm/broken-examples/unbounded-cache`

### Regex pattern compiled on hot path

**Type:** Performance issue · **Severity:** High · **Difficulty:** Basic

Compiling a regular expression pattern on every invocation of a hot loop wastes CPU cycles parsing
the regex syntax and creates high young-generation allocation churn. Precompile the pattern once into
a static final field.

**Appears in:** `modules/02-jvm/broken-examples/excessive-hot-path-allocation`

### Excessive young-gen allocation in telemetry formatting

**Type:** Performance issue · **Severity:** High · **Difficulty:** Intermediate

Using Stream pipelines, intermediate collections, lambdas and `String.format` inside high-throughput
formatting paths creates high TLAB churn and frequent minor GCs. Use a single operation-local
`StringBuilder` and direct character traversal.

**Appears in:** `modules/02-jvm/broken-examples/excessive-hot-path-allocation`

### Blocking Servlet Worker Thread in Controller

**Type:** Performance issue · **Severity:** High · **Difficulty:** Intermediate

Performing long-running, multi-second calculations or un-timed blocking downstream calls directly on the Servlet worker thread starves the Tomcat HTTP thread pool under concurrent load. Offload long operations asynchronously via `DeferredResult` or `CompletableFuture` using a dedicated bounded `ExecutorService`.

### Unbounded REST Query & Deep Offset Degradation

**Type:** Performance issue · **Severity:** Critical · **Difficulty:** Intermediate

Querying and returning full collections without bounds causes JVM `OutOfMemoryError` heap crashes and DB connection starvation. Deep offset pagination (`OFFSET N LIMIT M`) incurs $O(N)$ scanning costs on every request. Enforce hard page size limits (`@Max(100)`) and adopt keyset cursor pagination.

**Appears in:** `modules/10-rest-api/broken-examples/missing-pagination-unbounded-list`

### Cache Stampede on Hot Key Expiration (Thundering Herd)

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Intermediate

When a high-traffic cached resource expires, all concurrent incoming requests observe a cache miss simultaneously. Hundreds of application threads bypass the cache and run expensive database joins concurrently, exhausting database connection pools and causing system-wide timeouts. Implement single-flight mutex locking (distributed lock via `SET key token NX PX`) or probabilistic early refresh (XFetch).

**Appears in:** `modules/13-caching-redis/broken-examples/cache-stampede-on-expiry`

---

### Missing Scheduler Offloading for Blocking I/O Causing Event Loop Starvation

**Type:** Performance issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** Spring WebFlux, Project Reactor, Schedulers · **Interview frequency:** High · **Production impact:** Critical

Wrapping synchronous blocking operations inside `Mono.fromCallable` or `Mono.fromSupplier` without chaining `.subscribeOn(Schedulers.boundedElastic())` causes the blocking work to execute directly on the subscriber's thread (Netty worker). Even a modest load of 10–20 concurrent slow queries exhausts the few available event loops, resulting in catastrophic latency degradation across the entire WebFlux application.

**Appears in:** `modules/21-webclient-webflux/broken-examples/blocking-jdbc-in-webflux`

---

### Uncontrolled flatMap Concurrency Oversubscribes Outbound Connection Pool

**Type:** Scalability issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Project Reactor, WebClient, Netty Connection Pool · **Interview frequency:** High · **Production impact:** Critical

By default, `Flux.flatMap(Function)` uses an internal prefetch and concurrency limit of `Queues.SMALL_BUFFER_SIZE` (256 concurrent subscribers). When processing large collections of items (e.g. 5,000 notifications), `flatMap` eagerly spins up 256 parallel outbound HTTP requests. If connection pools are capped lower or downstream services are rate-limited, requests fail with `PoolAcquireTimeoutException` or trigger 429/503 errors. Always supply an explicit concurrency limit via `flatMap(mapper, maxConcurrency)`.

**Appears in:** `modules/21-webclient-webflux/broken-examples/uncontrolled-flatmap-concurrency`

---

### High-Cardinality Tags Cause MetricRegistry Memory Explosion

**Type:** Performance issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** Micrometer, Prometheus, JVM Memory · **Interview frequency:** High · **Production impact:** Critical

In Micrometer, meter instances (`Counter`, `Timer`, `Gauge`) are cached permanently in the `MeterRegistry`'s internal map indexed by their unique combination of name and tag key-values. Tagging metrics with high-cardinality attributes like `order_id` (UUID), `user_id`, or `email` causes the registry to retain millions of meter objects. Over time, this leads to continuous tenured heap bloat, major garbage collection pauses, and eventually an unrecoverable `OutOfMemoryError`. Keep metric dimensions strictly bounded to low-cardinality enums ($< 100$ combinations).

**Appears in:** `modules/22-observability/broken-examples/high-cardinality-metric-tags`

### Connection Retained Across Remote I/O

**Type:** Resource leak issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** HikariCP, JDBC · **Interview frequency:** High · **Production impact:** High

A checked-out connection sits idle while a payment call runs. Keep remote I/O outside the
connection scope and budget pool capacity across instances. Detect it with Hikari active, pending,
and timeout metrics. The corrected workflow needs explicit state and reconciliation.

**Appears in:** `modules/23-performance/broken-examples/hikari-exhaustion`

### CPU Pool Greatly Exceeds Available Processors

**Type:** Performance issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** ExecutorService · **Interview frequency:** High · **Production impact:** High

Hundreds of runnable CPU workers increase scheduling and cache contention. Size from processors and
measured blocking ratio, then verify with throughput, p99, runnable threads, and context switches.

**Appears in:** `modules/23-performance/broken-examples/oversized-thread-pool`

### Unbounded Executor Queue Retains Overload

**Type:** Scalability issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** ThreadPoolExecutor · **Interview frequency:** High · **Production impact:** High

An unbounded queue converts overload into growing latency and retained heap. Bound capacity and
reject explicitly; monitor queue depth, oldest task age, and rejection rate.

**Appears in:** `modules/23-performance/broken-examples/oversized-thread-pool`

### Duplicate Batch Keys Repeat Database I/O

**Type:** Performance issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** SQL batching · **Interview frequency:** Medium · **Production impact:** Medium

Repeated foreign keys trigger identical lookups. Deduplicate keys before one bounded batch query
and confirm the improvement with statement counts and traces.

**Appears in:** `modules/23-performance/broken-examples/n-plus-one-summary`

### Hot-Path Intermediate Allocation Chain

**Type:** Performance issue · **Severity:** High · **Difficulty:** Senior

**Technology:** Java streams, GC · **Interview frequency:** High · **Production impact:** High

Intermediate maps, regex objects, formatted strings, and boxing raise allocation rate and young-GC
pressure. Use JFR allocation profiles before replacing the measured path with one local builder.

**Appears in:** `modules/23-performance/broken-examples/excessive-allocation`

### Regular Expression Compiled Per Event

**Type:** Performance issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Java regex · **Interview frequency:** Medium · **Production impact:** High

Compiling an invariant pattern for every event repeats parsing and allocation at traffic rate.
Precompile immutable patterns or use a focused character loop, and confirm the effect in a JFR
allocation and CPU profile.

**Appears in:** `modules/23-performance/broken-examples/excessive-allocation`

### General Formatter Used in a Fixed Hot Path

**Type:** Performance issue · **Severity:** Medium · **Difficulty:** Intermediate

**Technology:** Java formatting · **Interview frequency:** Medium · **Production impact:** Medium

`String.format` provides a general parser and formatter for a fixed protocol shape. Direct appends
can reduce CPU and temporary objects when a representative profile shows the path is material; wire
format tests preserve correctness.

**Appears in:** `modules/23-performance/broken-examples/excessive-allocation`

### Boxed Counter Replacement Under Contention

**Type:** Performance issue · **Severity:** Medium · **Difficulty:** Intermediate

**Technology:** Java boxing, LongAdder · **Interview frequency:** Medium · **Production impact:** Medium

Replacing immutable boxed values allocates on every update while holding a shared monitor. A
per-key striped primitive counter removes both sources of pressure when weakly consistent telemetry
reads are acceptable.

**Appears in:** `modules/23-performance/broken-examples/lock-contention`

### Global Monitor for Independent Counters

**Type:** Scalability issue · **Severity:** High · **Difficulty:** Senior

**Technology:** Java monitors, LongAdder · **Interview frequency:** High · **Production impact:** High

One monitor serializes independent keys and readers. Per-key striped counters reduce the hot spot;
JFR monitor-blocked events and throughput by concurrency level show the effect.

**Appears in:** `modules/23-performance/broken-examples/lock-contention`

---

### Single-Stage Fat Container Image Bloats Deployment and Startup

**Type:** Performance issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Docker, BuildKit, Layer Caching, Registry Bandwidth · **Interview frequency:** High · **Production impact:** High

Compiling and packaging Java applications inside a single-stage Dockerfile bundles compiler toolchains (full JDK), Gradle/Maven caches, source trees, and build tools into the runtime image (producing 800MB–1.5GB images). This bloats container registry storage, consumes gigabytes of CI/CD network egress, dramatically slows Kubernetes node image pulls during autoscaling events, and enlarges the container vulnerability surface. Use multi-stage builds separating the build environment from a minimal runtime JRE/distroless image, and extract Spring Boot layered JARs to maximize Docker layer caching.

**Appears in:** `modules/24-docker/broken-examples/fat-image-root-user`

---

### Off-Heap Native Memory Access Without Bounds Checks

**Type:** Memory issue · **Severity:** Critical · **Difficulty:** Intermediate

**Track:** `java25-boot4` · **Technology:** `sun.misc.Unsafe`, Pointer Arithmetic · **Interview frequency:** High · **Production impact:** Critical

Using raw pointer arithmetic without validating indices allows reads and writes beyond allocated buffer boundaries. Writing outside allocated off-heap space silently overwrites unrelated native memory structures, leading to corrupt payloads, fatal SIGSEGV process crashes, or exploitable memory safety vulnerabilities. Always enforce strict spatial bounds checks via the Foreign Function & Memory API (`MemorySegment`).

**Appears in:** [Java 25 / Boot 4 What's New — Unsafe Off-Heap Buffer](../tracks/java25-boot4/whats-new/code-review.md#review-target-1-unsafe-off-heap-buffer)

---

### Native Off-Heap Memory Leak Without AutoCloseable Lifecycle

**Type:** Resource leak issue · **Severity:** Critical · **Difficulty:** Intermediate

**Track:** `java25-boot4` · **Technology:** Native Memory, `Arena`, `AutoCloseable` · **Interview frequency:** High · **Production impact:** Critical

Allocating native off-heap memory without binding it to a deterministic owner or `AutoCloseable` lifecycle causes native memory to leak for the life of the OS process if an exception escapes before manual deallocation or if a caller forgets to invoke cleanup. Because off-heap memory is invisible to JVM garbage collection pauses, the process eventually exhausts host RAM and is killed by the OS Out-Of-Memory (OOM) killer. Use `Arena.ofConfined()` within `try-with-resources`.

**Appears in:** [Java 25 / Boot 4 What's New — Unsafe Off-Heap Buffer](../tracks/java25-boot4/whats-new/code-review.md#review-target-1-unsafe-off-heap-buffer)

## Related

- [Issue catalogue](index.md)
