# Solution — Missing Latency Histogram and Tail Outliers

## Annotated code

```java
package lab.observability.broken.latencyhistogram;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class CheckoutLatencyTracker {

    private final Timer checkoutTimer;
    private final AtomicLong totalDurationMs = new AtomicLong(0);
    private final AtomicLong totalCount = new AtomicLong(0);

    public CheckoutLatencyTracker(MeterRegistry meterRegistry) {
        // Observability issue: Default Timer does not publish percentiles or histogram buckets, masking tail latency.
        // Observability issue: No SLO boundaries defined for automated alerting against service level agreements.
        this.checkoutTimer = meterRegistry.timer("checkout.duration");
    }

    public void recordCheckout(Duration duration) {
        // Observability issue: Tracking arithmetic mean (sum/count) completely obscures bimodal latency spikes.
        checkoutTimer.record(duration);
        totalDurationMs.addAndGet(duration.toMillis());
        totalCount.incrementAndGet();
    }

    public double getAverageLatencyMs() {
        long count = totalCount.get();
        return count == 0 ? 0.0 : (double) totalDurationMs.get() / count;
    }
}
```

## Issue list

### Observability issue: Arithmetic mean latency obscures severe tail latency spikes

- **Location:** `CheckoutLatencyTracker.java:23`
- **Description:** Using an arithmetic average (`totalDurationMs / totalCount`) gives an illusion of health. If 99 requests take 10ms and 1 request hangs for 10,000ms, the reported average is ~109ms, hiding a catastrophic 10-second stall experienced by 1% of users.
- **Impact:** Critical p99 degradations, lock contention, and GC pauses escape notice because the average appears normal.
- **Remediation:** Configure `Timer.builder("checkout.duration").publishPercentiles(0.5, 0.95, 0.99)` or `publishPercentileHistogram()`.

### Observability issue: Missing Service Level Objective (SLO) histogram boundaries

- **Location:** `CheckoutLatencyTracker.java:18`
- **Description:** Without explicit histogram buckets (`serviceLevelObjectives(Duration.ofMillis(200), Duration.ofMillis(500), Duration.ofSeconds(1))`), Prometheus cannot compute rate-based SLO compliance (e.g., "$99\%$ of requests complete under $500\text{ms}$").
- **Impact:** Inability to configure accurate multi-window multi-burn-rate alerting recommended by Google SRE standards.
- **Remediation:** Configure `serviceLevelObjectives(...)` on the `Timer.builder`.

## Correct implementation

See [`lab.observability.latencyhistogram.CorrectCheckoutLatencyTracker`](../../src/main/java/lab/observability/latencyhistogram/CorrectCheckoutLatencyTracker.java).

Detailed discussion in [Solutions](../../../docs/topics/observability/solutions.md#5-accurate-percentiles-and-slo-histograms).
