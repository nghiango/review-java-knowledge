# Concurrency Tests

## Concurrency Testing Strategy

Testing concurrent code requires techniques that reliably reproduce race conditions, interleavings, and deadlocks:

1. **Start & End Gate Coordination**: Uses `CountDownLatch startGate = new CountDownLatch(1)` so all worker threads block until `startGate.countDown()` releases them simultaneously, maximizing concurrent contention.
2. **Stress Loop Repetitions**: Tests execute hundreds or thousands of iterations per thread across thread pools to detect timing windows.
3. **Deterministic Assertions**: Verify exact invariants (e.g. Total balance conserved, exact count increments, zero overselling).
4. **No Arbitrary Sleeping**: Tests avoid `Thread.sleep` in assertions, using bounded `await()` on `CountDownLatch` or `Awaitility`.

## Test Suite Overview

```bash
./gradlew :modules:03-concurrency:test
```

| Test Class | Invariant Proved | Technique |
|---|---|---|
| `UserVisitMetricsServiceTest` | Exact total count preserved under concurrent increments | 16 parallel threads, `CountDownLatch` start/end gate |
| `DeadlockFreeTransferServiceTest` | Absence of deadlock and balance conservation during bidirectional transfers | Simultaneous opposite transfers ($A \to B$ and $B \to A$) |
| `InventoryReservationServiceTest` | Flash-sale stock never drops below 0 and never oversells | 50 concurrent buyers competing for 33 valid reservations |
| `BoundedJobProcessorTest` | Bounded pool execution and two-phase graceful shutdown | Queue overflow and `gracefulShutdown()` draining |
| `AsyncCustomerDashboardServiceTest` | Non-blocking fan-out and partial failure resilience | Mocked slow clients, `CompletableFuture.get(timeout)` |

## Key Test Snippets

### Proving absence of lost updates under contention

```java
--8<-- "modules/03-concurrency/src/test/java/lab/concurrency/lostupdate/UserVisitMetricsServiceTest.java"
```

### Proving deadlock-free bidirectional transfers

```java
--8<-- "modules/03-concurrency/src/test/java/lab/concurrency/lockordering/DeadlockFreeTransferServiceTest.java"
```

### Proving flash-sale inventory consistency

```java
--8<-- "modules/03-concurrency/src/test/java/lab/concurrency/volatilecompound/InventoryReservationServiceTest.java"
```

## Related

- [Solutions](solutions.md)
- [Code Review](code-review.md)
- [Production Diagnostics](production.md)
