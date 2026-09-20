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

## Related

- [Issue catalogue](index.md)
