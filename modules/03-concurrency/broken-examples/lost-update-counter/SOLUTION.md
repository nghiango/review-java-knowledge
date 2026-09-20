# Solution: Lost update on shared counter and metrics state

## Annotated code

```java
public class HitCounter {
    // Concurrency issue: Plain long field is neither volatile nor atomic; reads across threads
    // have no visibility guarantee and 64-bit updates can even suffer non-atomic word tearing on 32-bit JVMs.
    private long totalHits;

    // Concurrency issue: totalHits++ is a non-atomic read-modify-write (read, increment, write).
    // Under concurrent execution, interleaved operations lose updates.
    public void recordHit() {
        totalHits++;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public void reset() {
        totalHits = 0;
    }
}

public class UserVisitMetricsService {
    private final HitCounter hitCounter = new HitCounter();

    // Concurrency issue: HashMap is not thread-safe. Concurrent get and put calls lead to lost updates,
    // corrupted internal bucket linked-lists/trees, and infinite loops in legacy rehashing.
    private final Map<String, Integer> visitsPerUser = new HashMap<>();

    // Concurrency issue: Non-volatile boolean flag. Writes by shutdown() on one thread may never become
    // visible to worker threads due to JMM caching and compiler loop hoisting.
    private boolean active = true;

    public void recordUserHit(String userId) {
        if (!active) {
            return;
        }
        hitCounter.recordHit();

        // Concurrency issue: Check-then-act / getOrDefault followed by put is non-atomic and races with
        // other threads modifying the same user entry.
        Integer count = visitsPerUser.getOrDefault(userId, 0);
        visitsPerUser.put(userId, count + 1);
    }

    // Performance issue: Holding the intrinsic lock (synchronized monitor) while sleeping or executing slow I/O
    // serializes all audit callers and severely degrades system throughput.
    public synchronized void recordAuditVisit(String userId) {
        if (!active) {
            return;
        }
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        recordUserHit(userId);
    }

    public void shutdown() {
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }

    public long getTotalHits() {
        return hitCounter.getTotalHits();
    }

    public Map<String, Integer> getVisitsPerUser() {
        return Collections.unmodifiableMap(visitsPerUser);
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Concurrency issue | Critical | `HitCounter.totalHits` | Plain `long` without visibility or atomic guarantees |
| 2 | Concurrency issue | Critical | `HitCounter.recordHit()` | Non-atomic `++` compound read-modify-write loses updates |
| 3 | Concurrency issue | Critical | `UserVisitMetricsService.visitsPerUser` | Non-thread-safe `HashMap` accessed by concurrent threads |
| 4 | Concurrency issue | High | `UserVisitMetricsService.active` | Non-volatile flag risks stale reads and visibility failure |
| 5 | Concurrency issue | Critical | `UserVisitMetricsService.recordUserHit()` | Compound check-then-act map update loses concurrent increments |
| 6 | Performance issue | High | `UserVisitMetricsService.recordAuditVisit()` | Holding intrinsic monitor across blocking `sleep` serializes callers |

## Issue details

### Compound read-modify-write lost updates

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Basic

Java primitive increments (`count++`) are three separate bytecode instructions: `GETFIELD`, `IADD`, `PUTFIELD`. If two threads execute concurrently, both read the same initial value (e.g., 5), increment to 6 in local registers, and write back 6, losing one increment. Replace with `AtomicLong` (for low-to-medium contention) or `LongAdder` (for high-contention throughput accumulators).

### Plain HashMap in multithreaded context

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Intermediate

`HashMap` provides zero synchronization. Concurrent `put()` calls resize buckets and rewrite table nodes simultaneously, resulting in missing entries and race conditions. Replace with `ConcurrentHashMap` and use atomic operations such as `compute(userId, (k, v) -> v == null ? 1 : v + 1)` or `merge(userId, 1, Integer::sum)`.

### Missing happens-before visibility on control flag

**Type:** Concurrency issue · **Severity:** High · **Difficulty:** Intermediate

Without `volatile` or atomic references, writes to `active` by one thread have no *happens-before* relationship with reads by another thread. The JIT compiler can hoist the read out of loops or read from CPU L1/L2 cache, causing threads to process requests indefinitely after `shutdown()`. Use `volatile boolean` or `AtomicBoolean`.

### Slow blocking call inside synchronized lock

**Type:** Performance issue · **Severity:** High · **Difficulty:** Intermediate

Holding an object monitor during blocking operations (`sleep`, network calls, disk I/O) prevents any other thread from entering any synchronized method on the instance, leading to thread starvation and cascading latency. Synchronization should only enclose fast in-memory critical sections.

## Correct implementation

The production-ready fix lives in `lab.concurrency.lostupdate`:
- `AtomicHitCounter.java` using `LongAdder` for zero-contention scalable metrics.
- `StripedMetricsAccumulator.java` wrapping `ConcurrentHashMap` with atomic `compute` / `merge` updates.
- `UserVisitMetricsService.java` using `volatile boolean` shutdown state and lock-free atomic accumulators.
