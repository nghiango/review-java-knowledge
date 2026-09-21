# Concurrency Issues

Atomicity, visibility, ordering, shared mutation, deadlock and starvation failures.

## Entries

### Shared mutable accumulator in a parallel stream

**Type:** Concurrency issue · **Severity:** High · **Difficulty:** Intermediate

Parallel workers cannot safely append to one ArrayList. Use a pure mapping/collector whose reduction
owns its state, or submit independent values to an explicit executor and join immutable results.

**Appears in:** [Core Java — stream side effects](../topics/core-java/code-review.md#stream-and-parallel-side-effects)

### Non-atomic counter increment on shared mutable state

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Basic

Compound read-modify-write (`count++`) operations executed by multiple threads interleave and silently drop updates. Replace with `AtomicLong` or `LongAdder`.

**Appears in:** [Concurrency — lost update on shared counter](../topics/concurrency/code-review.md#lost-update-on-shared-counter)

### Deadlock from inconsistent lock acquisition order

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Intermediate

Acquiring multiple locks in arbitrary caller-dependent order causes circular wait deadlock under concurrent reverse operations. Enforce strict canonical resource ordering (such as comparable IDs).

**Appears in:** [Concurrency — lock ordering deadlock](../topics/concurrency/code-review.md#lock-ordering-deadlock)

### Volatile used for compound check-then-act

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Intermediate

`volatile` guarantees visibility and memory ordering but provides zero atomicity for check-then-act logic, causing race conditions and overselling. Replace with atomic CAS retry loops (`AtomicInteger.updateAndGet`).

**Appears in:** [Concurrency — volatile compound action](../topics/concurrency/code-review.md#volatile-compound-action)

### Unbounded thread pools and thread creation

**Type:** Resource management issue · **Severity:** Critical · **Difficulty:** Intermediate

Unbounded thread creation (`newCachedThreadPool()` or ad-hoc `new Thread()`) exhausts OS memory under load, causing JVM native thread OOM. Use a bounded `ThreadPoolExecutor` with a bounded queue and backpressure policy.

**Appears in:** [Concurrency — unbounded thread creation](../topics/concurrency/code-review.md#unbounded-thread-creation)

### Serialized `.join()` inside loops and common-pool starvation

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Intermediate

Invoking `.join()` on `CompletableFuture` inside each loop iteration eliminates parallelism and serializes execution. Running blocking I/O on `ForkJoinPool.commonPool()` starves CPU tasks. Use `CompletableFuture.allOf()` and dedicated I/O thread pools.

**Appears in:** [Concurrency — completable future unhandled join](../topics/concurrency/code-review.md#completablefuture-unhandled-join)

---

### Distributed Lock Lease Expiration During Client Pause (GC / Network Stall)

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Senior

**Technology:** Redis, Distributed Locking, etcd · **Interview frequency:** High · **Production impact:** Critical

When a worker acquires a distributed lock lease with a fixed TTL and experiences a Stop-The-World (STW) GC pause, hypervisor descheduling, or network buffer delay exceeding that TTL, the coordination service expires the lock and grants it to a second worker. When the first worker resumes, it executes writes concurrently with the new lock holder, corrupting shared storage. Protect distributed locks with monotonic fencing tokens validated at the storage boundary.

**Appears in:** `modules/17-distributed-systems/broken-examples/distributed-lock-no-fencing`

---

### Holding Database Row Locks Across Network Boundaries in Two-Phase Commit

**Type:** Concurrency issue · **Severity:** High · **Difficulty:** Senior

**Technology:** Two-Phase Commit, Distributed Transactions · **Interview frequency:** High · **Production impact:** Critical

Holding pessimistic database row locks (`SELECT ... FOR UPDATE`) while waiting for multiple network RPCs across independent microservices creates severe lock contention, thread starvation, and connection pool exhaustion. Any transient network delay or crash in a downstream participant blocks database rows across the entire cluster. Replace synchronous 2PC locking with asynchronous Saga orchestration and compensating transactions.

**Appears in:** `modules/17-distributed-systems/broken-examples/two-phase-commit-coordinator`

---

### Invoking .block() Inside WebFlux Request Flow Freezes Netty Event Loops

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** Spring WebFlux, Project Reactor, Netty · **Interview frequency:** High · **Production impact:** Critical

In Spring WebFlux, Netty runs a fixed number of event loop worker threads (typically 1 per CPU core) to service all concurrent incoming HTTP connections. Calling `.block()` or `.toFuture().get()` inside an event loop handler pauses that thread until the asynchronous publisher yields a value. If downstream latency spikes, all event loop threads quickly become blocked, causing the server to stop accepting new TCP connections and freezing all multiplexed requests.

**Appears in:** `modules/21-webclient-webflux/broken-examples/block-in-request-flow`

---

### Synchronous Blocking JDBC Executed on Reactive Netty Event Loop

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** Spring WebFlux, Spring JDBC, Project Reactor · **Interview frequency:** High · **Production impact:** Critical

Executing blocking JDBC queries (`JdbcClient`, `JdbcTemplate`, or Hibernate) directly inside `Mono.fromSupplier(...)` executes the query on the calling subscriber thread (the Netty event loop). Traditional JDBC drivers block the thread on socket reads and connection pool acquisition. Slow queries or HikariCP pool exhaustion instantly stall the event loop, causing severe latency spikes for completely unrelated HTTP requests. Offload blocking calls to `Schedulers.boundedElastic()` or adopt non-blocking R2DBC drivers.

**Appears in:** `modules/21-webclient-webflux/broken-examples/blocking-jdbc-in-webflux`

---

### Thread Pool Reuse Leaks Dirty MDC State Across Requests

**Type:** Concurrency issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** SLF4J MDC, Thread Pools, ExecutorService · **Interview frequency:** High · **Production impact:** High

Because `ThreadPoolExecutor` worker threads are continuously reused across requests, any value written to `MDC.put()` remains bound to the thread's `ThreadLocal` storage if not explicitly cleared. When that pooled worker thread executes a subsequent, unrelated task from a different user or tenant, it inherits the stale `correlationId` and `userId`. This causes cross-tenant log pollution where logs from User B appear tagged with User A's identifiers. Always wrap tasks in a try-finally block and invoke `MDC.clear()`.

**Appears in:** `modules/22-observability/broken-examples/missing-correlation-id-async`

### Independent Metric Keys Share One Monitor

**Type:** Concurrency issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Intrinsic locks, LongAdder · **Interview frequency:** High · **Production impact:** High

A synchronized accumulator forces unrelated writers through one monitor. Use per-key striped
counters when eventually observed sums are acceptable; diagnose with JFR monitor-blocked events.

**Appears in:** `modules/23-performance/broken-examples/lock-contention`

---

### Obsolete Virtual-Thread Pinning Workaround Kept After the Upgrade

**Type:** Concurrency issue · **Severity:** Medium · **Difficulty:** Intermediate

**Track:** `java25-boot4` · **Technology:** Virtual threads, ObjectMonitor · **Interview frequency:** High · **Production impact:** Medium

Rewriting `synchronized` into `ReentrantLock` to avoid carrier pinning was correct on Java 21 and is obsolete on Java 24+, where a virtual thread blocking inside a monitor unmounts normally. Keeping the workaround adds a heap-allocated lock and a manual release path that the compiler previously guaranteed, so the "fix" now carries more risk than the problem it solved. Revert such locks unless `tryLock`, fairness or multiple `Condition`s are genuinely required.

**Appears in:** [Java 25 / Boot 4 — obsolete pinning refactor](../tracks/java25-boot4/core-java/code-review.md#obsolete-pinning-refactor)

---

### Orphan Subtasks and Resource Leakage in Unstructured Concurrency

**Type:** Concurrency issue · **Severity:** High · **Difficulty:** Intermediate

**Track:** `java25-boot4` · **Technology:** Virtual threads, CompletableFuture, StructuredTaskScope · **Interview frequency:** High · **Production impact:** High

Subtasks forked using `CompletableFuture.supplyAsync()` or bare executors have independent lifecycles. If one subtask fails, sibling tasks continue executing as orphans in the background, consuming database connections, external bandwidth, and CPU. Use `StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())` to coordinate tasks as a single unit of work and automatically cancel sibling tasks upon failure.

**Appears in:** [Java 25 / Boot 4 Concurrency — Unstructured Concurrency & Orphan Task Leaks](../tracks/java25-boot4/concurrency/code-review.md#review-target-2-orderfulfillmentservicejava)

---

### InheritableThreadLocal Heap Bloat and Leaks in High-Throughput Virtual Threads

**Type:** Memory issue · **Severity:** High · **Difficulty:** Senior

**Track:** `java25-boot4` · **Technology:** Virtual threads, ThreadLocal, ScopedValue · **Interview frequency:** High · **Production impact:** High

`InheritableThreadLocal` clones thread-local maps into every child thread. Spawning millions of virtual threads causes severe heap bloat, while missing `finally { tl.remove(); }` causes context pollution across requests. Migrate to Java 25 `ScopedValue` for immutable, stack-confined context sharing with zero cleanup boilerplate.

**Appears in:** [Java 25 / Boot 4 Concurrency — Virtual Thread Context Propagation & Leaks](../tracks/java25-boot4/concurrency/code-review.md#review-target-1-tenantcontextholderjava)

---

### InheritableThreadLocal Security Context Pollution Across Worker Pools

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Senior

**Track:** `java25-boot4` · **Technology:** Spring Security 7, Virtual Threads, InheritableThreadLocal · **Interview frequency:** High · **Production impact:** Critical

`InheritableThreadLocal` creates subtle concurrency bugs when combined with reusable worker pools or virtual threads: tasks dispatched to existing threads do not re-inherit context, while uncleared credentials survive on the worker thread, causing subsequent tasks to run with unauthorized credentials. Replace with Java 25 `ScopedValue` or context-propagating executor decorators.

**Appears in:** [Java 25 / Boot 4 Spring Security — InheritableThreadLocal Context Pollution](../tracks/java25-boot4/spring-security/code-review.md#review-target-2-inheritablethreadlocal-security-context-pollution)

---

### Unbound Virtual Thread Dispatch in Tests

**Type:** Concurrency issue · **Severity:** Medium · **Difficulty:** Intermediate

**Track:** `java25-boot4` · **Technology:** Virtual threads, StructuredTaskScope, Test Execution · **Interview frequency:** Medium · **Production impact:** Medium

Spawning fire-and-forget virtual threads in unit tests without bounding execution scopes or joining tasks causes uncoordinated threads to outlive test methods. This pollutes subsequent test execution and masks silent background exceptions. Concurrency tests must coordinate task completion using `StructuredTaskScope` or managed countdown latches.

---

### Carrier Thread Pinning in Synchronized Resilience Monitors

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Senior

**Track:** `java25-boot4` · **Technology:** Virtual Threads, Circuit Breaker, Resilience · **Interview frequency:** High · **Production impact:** Critical

Using `synchronized` methods or monitors in resilience decorators (circuit breakers, rate limiters, retry loops) pins virtual threads to their OS carrier threads whenever contention or blocking delays occur. Under high load, all ForkJoinPool carrier threads become pinned, stalling the entire application. Replace synchronized monitors with lock-free atomic primitives (`AtomicReference`, `AtomicInteger`) or `ReentrantLock`.

**Appears in:** [Java 25 / Boot 4 Resilience — Carrier-Pinning Circuit Breaker](../tracks/java25-boot4/resilience/code-review.md#review-target-2-carrier-pinning-circuit-breaker)

---

## Related

- [Issue catalogue](index.md)
- [Distributed Systems topic documentation](../topics/distributed-systems/index.md)
