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
        // Standard unconfigured Timer without percentiles or SLO buckets
        this.checkoutTimer = meterRegistry.timer("checkout.duration");
    }

    public void recordCheckout(Duration duration) {
        checkoutTimer.record(duration);
        totalDurationMs.addAndGet(duration.toMillis());
        totalCount.incrementAndGet();
    }

    public double getAverageLatencyMs() {
        long count = totalCount.get();
        return count == 0 ? 0.0 : (double) totalDurationMs.get() / count;
    }
}
