# Solution: report executor sizing

## Annotated code

```java
package lab.performance.broken.threadpool;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class ReportExecutor implements AutoCloseable {
    // Performance issue: Five hundred CPU workers create scheduling and cache contention.
    // Scalability issue: The factory uses an unbounded queue, so overload becomes heap growth.
    private final ExecutorService executor = Executors.newFixedThreadPool(500);

    public List<Future<byte[]>> render(List<ReportJob> jobs) {
        // Reliability issue: The API accepts an unbounded batch and provides no rejection signal.
        return jobs.stream().map(job -> executor.submit(job::render)).toList();
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    public interface ReportJob {
        byte[] render();
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Performance issue | High | executor field | CPU pool greatly exceeds processors |
| 2 | Scalability issue | High | executor field | Queue is unbounded |
| 3 | Reliability issue | Medium | `render()` | Overload has no explicit outcome |

## Issue details

### CPU pool greatly exceeds processors
**Type:** Performance issue · **Severity:** High · **Difficulty:** Intermediate  
**Technology:** ExecutorService · **Interview frequency:** High · **Production impact:** High

**Problem:** runnable workers exceed CPU capacity. **Why it happens:** a fixed number ignores the
workload and host. **Production impact:** context switches rise while throughput flattens.
**Correct implementation:** derive workers from processors and measured blocking ratio.
**Trade-offs:** sizing must be revisited when work changes. **How to detect it:** runnable thread
count, CPU saturation, and JFR scheduling samples. **Interview follow-up:** How does blocking
coefficient affect sizing?

### Unbounded work queue
**Type:** Scalability issue · **Severity:** High · **Difficulty:** Intermediate  
**Technology:** ThreadPoolExecutor · **Interview frequency:** High · **Production impact:** High

**Problem:** accepted work can grow without a cap. **Why it happens:** `newFixedThreadPool` uses an
unbounded queue. **Production impact:** stale work retains arguments and results until OOM.
**Correct implementation:** bounded queue plus rejection. **Trade-offs:** callers must handle
backpressure. **How to detect it:** queue depth and oldest-task age. **Interview follow-up:** When
is CallerRunsPolicy appropriate?

### No overload contract
**Type:** Reliability issue · **Severity:** Medium · **Difficulty:** Senior  
**Technology:** overload control · **Interview frequency:** Medium · **Production impact:** High

**Problem:** the API promises acceptance regardless of capacity. **Why it happens:** capacity is
hidden. **Production impact:** latency becomes unbounded. **Correct implementation:** reject or
rate-limit before submission. **Trade-offs:** visible failures require retry policy. **How to
detect it:** rejection, queue delay, and timeout metrics. **Interview follow-up:** Which HTTP status
communicates temporary saturation?

## Correct implementation

See package `lab.performance.threadpool` and
[solutions](../../../docs/topics/performance/solutions.md#bounded-workload-aware-executors).
