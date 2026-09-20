# Solution: metric accumulator contention

## Annotated code

```java
package lab.performance.broken.contention;

import java.util.HashMap;
import java.util.Map;

public final class MetricAccumulator {
    private final Map<String, Long> totals = new HashMap<>();

    // Concurrency issue: One monitor serializes independent metric keys.
    // Performance issue: Every update boxes a new Long while holding the global lock.
    public synchronized void add(String metric, long delta) {
        totals.put(metric, totals.getOrDefault(metric, 0L) + delta);
    }

    // Scalability issue: Reads contend with every writer on the same monitor.
    public synchronized long total(String metric) {
        return totals.getOrDefault(metric, 0L);
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Concurrency issue | High | `add()` | Independent keys share one monitor |
| 2 | Performance issue | Medium | `add()` | Updates box values under lock |
| 3 | Scalability issue | High | `total()` | Readers serialize with writers |

## Issue details

### Independent keys share one monitor
**Type:** Concurrency issue · **Severity:** High · **Difficulty:** Intermediate  
**Technology:** intrinsic locks · **Interview frequency:** High · **Production impact:** High

**Problem:** unrelated updates cannot proceed concurrently. **Why it happens:** synchronization is
on the accumulator instance. **Production impact:** monitor wait grows with request concurrency.
**Correct implementation:** concurrent map plus per-key `LongAdder`. **Trade-offs:** `sum()` is not
an atomic snapshot across keys. **How to detect it:** JFR monitor-blocked events and thread dumps.
**Interview follow-up:** When is `AtomicLong` better?

### Boxing under lock
**Type:** Performance issue · **Severity:** Medium · **Difficulty:** Intermediate  
**Technology:** allocation · **Interview frequency:** Medium · **Production impact:** Medium

**Problem:** each update creates boxed state. **Why it happens:** immutable `Long` values are
replaced. **Production impact:** allocation adds GC work to a contended path. **Correct
implementation:** mutable striped counters. **Trade-offs:** extra cells under contention. **How to
detect it:** allocation profiling by class. **Interview follow-up:** How does LongAdder stripe?

### Reads serialize with writers
**Type:** Scalability issue · **Severity:** High · **Difficulty:** Senior  
**Technology:** concurrent collections · **Interview frequency:** Medium · **Production impact:** High

**Problem:** scraping totals pauses updates and vice versa. **Why it happens:** both methods use one
monitor. **Production impact:** monitoring itself worsens saturation. **Correct implementation:**
per-key concurrent reads. **Trade-offs:** values are eventually observed during concurrent writes.
**How to detect it:** profile during metric scrapes. **Interview follow-up:** When is a consistent
snapshot required?

## Correct implementation

See package `lab.performance.contention` and
[solutions](../../../docs/topics/performance/solutions.md#contention-friendly-accumulation).
