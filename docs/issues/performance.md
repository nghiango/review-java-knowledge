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

## Related

- [Issue catalogue](index.md)
