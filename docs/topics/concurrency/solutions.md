# Concurrency Solutions

Correct, production-grade implementations corresponding to the code review exercises.

## Lock-free hit counter and metrics

### Implementation

```java
--8<-- "modules/03-concurrency/src/main/java/lab/concurrency/lostupdate/AtomicHitCounter.java"
```

```java
--8<-- "modules/03-concurrency/src/main/java/lab/concurrency/lostupdate/StripedMetricsAccumulator.java"
```

```java
--8<-- "modules/03-concurrency/src/main/java/lab/concurrency/lostupdate/UserVisitMetricsService.java"
```

### Why it works

1. **`LongAdder` Cell Striping**: Instead of all threads contending on a single memory location with CAS retries (`AtomicLong`), `LongAdder` allocates a dynamically resized cell array. Contending threads update separate cells via thread hash probes, eliminating memory bus contention.
2. **`ConcurrentHashMap` with atomic `computeIfAbsent`**: Guarantees atomic insertion and update of per-user counters without global locking.
3. **`AtomicBoolean` lifecycle flag**: Provides JMM happens-before guarantees when transitioning service state between active and shutdown.

### Trade-offs

`LongAdder.sum()` provides an eventual snapshot across cells rather than a point-in-time linearizable atomic total. In high-throughput metrics and telemetry, this trade-off dramatically improves write throughput with zero lock contention.

---

## Deadlock-free account transfer

### Implementation

```java
--8<-- "modules/03-concurrency/src/main/java/lab/concurrency/lockordering/Account.java"
```

```java
--8<-- "modules/03-concurrency/src/main/java/lab/concurrency/lockordering/DeadlockFreeTransferService.java"
```

### Why it works

1. **Deterministic Lock Ordering**: Comparing `from.compareTo(to)` ensures that all threads acquire locks on shared accounts in identical lexicographical order. Even with concurrent reverse transfers ($A \to B$ and $B \to A$), both threads compete for Account $A$'s lock first, eliminating circular wait (the fourth Coffman condition).
2. **Timed `tryLock` Acquisition**: Prevents indefinite blocking if an external anomaly delays lock release.
3. **Decoupled Asynchronous Notifications**: The external notification callback is triggered after releasing all locks, reducing lock hold duration to sub-microsecond levels.

### Trade-offs

Enforcing a global ordering requires resources to possess natural comparable identifiers (`accountId`) or consistent tie-breaker ordering (e.g. `System.identityHashCode`).

---

## Atomic inventory reservation

### Implementation

```java
--8<-- "modules/03-concurrency/src/main/java/lab/concurrency/volatilecompound/ItemStock.java"
```

```java
--8<-- "modules/03-concurrency/src/main/java/lab/concurrency/volatilecompound/InventoryReservationService.java"
```

### Why it works

1. **Lock-Free CAS Loop**: `availableQuantity.compareAndSet(current, updated)` ensures that stock reduction occurs atomically only when the available balance satisfies the requested quantity. If an intervening reservation changes the quantity, the loop retries against the latest value.
2. **`ConcurrentHashMap`**: Manages concurrent item registration and lookups safely.

### Trade-offs

Under extreme flash-sale contention on a single item, CAS loops consume CPU cycles in retry spinning. For massive burst contention, batching reservations or queuing requests into a single actor/event loop can reduce CPU churn.

---

## Bounded executor job processor

### Implementation

```java
--8<-- "modules/03-concurrency/src/main/java/lab/concurrency/unboundedthreads/CustomThreadPoolFactory.java"
```

```java
--8<-- "modules/03-concurrency/src/main/java/lab/concurrency/unboundedthreads/BoundedJobProcessor.java"
```

### Why it works

1. **Bounded Resource Limits**: Explicit `corePoolSize`, `maxPoolSize`, and bounded `ArrayBlockingQueue` prevent native thread exhaustion and heap `OutOfMemoryError`.
2. **`CallerRunsPolicy` Backpressure**: When the queue overflows, the submitting thread executes the task itself, naturally slowing down the upstream producer.
3. **Two-Phase Graceful Shutdown**: `shutdown()` stops new submissions while allowing in-flight tasks to finish within a bounded timeout before resorting to `shutdownNow()`.

### Trade-offs

`CallerRunsPolicy` blocks the submitting thread (e.g., HTTP request handler), which pushes backpressure upstream to clients and load balancers, protecting the JVM from catastrophic collapse.

---

## Non-blocking dashboard assembly

### Implementation

```java
--8<-- "modules/03-concurrency/src/main/java/lab/concurrency/asyncpipeline/AsyncCustomerDashboardService.java"
```

### Why it works

1. **Parallel Non-Blocking Fan-Out**: Collects all price fetching stages into a list of futures and combines them with `CompletableFuture.allOf(...)`.
2. **Dedicated I/O Executor**: Prevents blocking remote calls from starving the shared `ForkJoinPool.commonPool()`.
3. **Deadlines & Graceful Degradation**: `.orTimeout(3, TimeUnit.SECONDS)` and `.exceptionally(ex -> fallback)` guarantee that slow or failing dependencies return default values instead of hanging the entire request.

### Trade-offs

`CompletableFuture.allOf()` returns `CompletableFuture<Void>`, requiring completed values to be gathered from completed futures or captured in thread-safe collections.

## Related

- [Concurrency Tests](tests.md)
- [Production Diagnostics](production.md)
- [Concepts](concepts.md)
