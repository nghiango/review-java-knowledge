# Concurrency Code Review

Review each clean source before expanding its answer.

## Lost update on shared counter

A high-traffic analytics and hit tracking service loses metrics and experiences thread serialization.

```java
--8<-- "modules/03-concurrency/broken-examples/lost-update-counter/HitCounter.java"
```

```java
--8<-- "modules/03-concurrency/broken-examples/lost-update-counter/UserVisitMetricsService.java"
```

Consider primitive read-modify-write atomicity, thread safety of collections, happens-before visibility on flags, and holding locks across blocking delays.

??? warning "Reveal issues"
    **Concurrency issue — non-atomic `totalHits++`:** The compound read-increment-write is not atomic; concurrent threads overwrite each other's increments. Fix with `LongAdder` or `AtomicLong`.

    **Concurrency issue — plain `HashMap` in concurrent context:** `HashMap` is not thread-safe and corrupts internal bins under concurrent `put()` calls. Fix with `ConcurrentHashMap` and atomic compute operations.

    **Concurrency issue — non-volatile control flag:** Writes to `active` lack a happens-before relationship with reader threads. Fix with `AtomicBoolean` or `volatile boolean`.

    **Performance issue — sleep inside synchronized method:** Holding an intrinsic monitor during sleep or slow I/O blocks all threads attempting to record audit hits. Fix by synchronizing only fast in-memory updates and offloading audits asynchronously.

[Correct implementation](solutions.md#lock-free-hit-counter-and-metrics)

---

## Lock ordering deadlock

A peer-to-peer balance transfer service deadlocks under concurrent bidirectional transfers.

```java
--8<-- "modules/03-concurrency/broken-examples/lock-ordering-deadlock/Account.java"
```

```java
--8<-- "modules/03-concurrency/broken-examples/lock-ordering-deadlock/AccountTransferService.java"
```

Consider resource acquisition ordering, timeout recovery, and holding locks across external network calls.

??? warning "Reveal issues"
    **Concurrency issue — inconsistent lock ordering:** Locking `from` followed by `to` causes circular wait deadlock when Thread 1 transfers $A \to B$ while Thread 2 transfers $B \to A$. Fix by acquiring locks in deterministic global order (e.g. sorted by `accountId`).

    **Concurrency issue — unbounded wait with intrinsic monitors:** Intrinsic `synchronized` locks cannot time out or abort if deadlock occurs. Fix with `ReentrantLock.tryLock(timeout, unit)`.

    **Performance issue — network I/O inside lock:** Calling `auditClient.sendNotification()` while holding account locks inflates lock contention from microseconds to hundreds of milliseconds. Fix by releasing locks before firing external notifications.

[Correct implementation](solutions.md#deadlock-free-account-transfer)

---

## Volatile compound action

An inventory reservation service experiences overselling and negative stock balances during flash sales.

```java
--8<-- "modules/03-concurrency/broken-examples/volatile-compound-action/ItemStock.java"
```

```java
--8<-- "modules/03-concurrency/broken-examples/volatile-compound-action/InventoryReservationService.java"
```

Consider `volatile` guarantees vs limitations, check-then-act race conditions, and thread interruption handling.

??? warning "Reveal issues"
    **Concurrency issue — volatile on compound action:** `volatile` guarantees memory visibility but does not make check-then-act sequences atomic. Multiple threads pass `availableQuantity >= requested` concurrently, driving stock negative. Fix with atomic compare-and-swap CAS retry loops via `AtomicInteger`.

    **Concurrency issue — swallowed `InterruptedException`:** Catching and ignoring `InterruptedException` destroys the thread cancellation signal. Fix by re-asserting `Thread.currentThread().interrupt()`.

    **Concurrency issue — non-thread-safe item map:** The items map uses unsynchronized `HashMap`. Fix with `ConcurrentHashMap`.

[Correct implementation](solutions.md#atomic-inventory-reservation)

---

## Unbounded thread creation

A background job processing service crashes under peak load with `OutOfMemoryError: unable to create native thread`.

```java
--8<-- "modules/03-concurrency/broken-examples/unbounded-thread-creation/ReportGenerationWorker.java"
```

```java
--8<-- "modules/03-concurrency/broken-examples/unbounded-thread-creation/BackgroundJobProcessor.java"
```

Consider thread pool sizing, queue limits, backpressure policies, and graceful two-phase shutdown.

??? warning "Reveal issues"
    **Resource management issue — unbounded `newCachedThreadPool()`:** Configured with `Integer.MAX_VALUE` maximum threads, traffic spikes spawn thousands of OS threads and exhaust native memory. Fix with a bounded `ThreadPoolExecutor` and fixed queue.

    **Resource management issue — ad-hoc `new Thread()`:** Spawning unmanaged platform threads bypasses all concurrency limits and metrics. Fix by routing all tasks through managed executor pools.

    **Reliability issue — abrupt `shutdownNow()`:** Hard termination interrupts tasks abruptly and drops queued work. Fix with standard two-phase graceful shutdown (`shutdown()` $\to$ `awaitTermination()` $\to$ fallback `shutdownNow()`).

[Correct implementation](solutions.md#bounded-executor-job-processor)

---

## CompletableFuture unhandled join

An asynchronous dashboard aggregation service exhibits high latency and common-pool thread starvation.

```java
--8<-- "modules/03-concurrency/broken-examples/completable-future-unhandled-join/PricingClient.java"
```

```java
--8<-- "modules/03-concurrency/broken-examples/completable-future-unhandled-join/CustomerDashboardService.java"
```

Consider non-blocking composition, thread pool isolation for I/O tasks, timeout deadlines, and resilient error fallbacks.

??? warning "Reveal issues"
    **Concurrency issue — `.join()` inside loop:** Calling `.join()` sequentially inside each loop iteration serializes asynchronous requests, defeating parallelism. Fix with `CompletableFuture.allOf()`.

    **Performance issue — blocking I/O on `ForkJoinPool.commonPool()`:** Executing remote network calls on `commonPool` starves JVM-wide CPU tasks and parallel streams. Fix with a dedicated bounded I/O executor.

    **Reliability issue — missing timeouts and fallbacks:** A single downstream stall or exception crashes the entire dashboard pipeline. Fix with `.orTimeout(timeout, unit)` and `.exceptionally(...)` graceful fallbacks.

[Correct implementation](solutions.md#non-blocking-dashboard-assembly)

## Related

- [Concurrency Solutions](solutions.md)
- [Concurrency Tests](tests.md)
- [Issue Catalogue](../../issues/concurrency.md)
