package lab.observability.latencyhistogram;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class CorrectCheckoutLatencyTracker {

    private final Timer checkoutTimer;

    public CorrectCheckoutLatencyTracker(MeterRegistry meterRegistry) {
        // High-precision latency timer with p50, p95, p99, p999 percentiles and explicit SLA/SLO
        // histogram buckets
        this.checkoutTimer =
                Timer.builder("checkout.duration")
                        .description("Tracks checkout duration and tail latency distribution")
                        .publishPercentiles(0.5, 0.95, 0.99, 0.999)
                        .publishPercentileHistogram()
                        .serviceLevelObjectives(
                                Duration.ofMillis(50),
                                Duration.ofMillis(100),
                                Duration.ofMillis(250),
                                Duration.ofMillis(500),
                                Duration.ofSeconds(1),
                                Duration.ofSeconds(2))
                        .minimumExpectedValue(Duration.ofMillis(1))
                        .maximumExpectedValue(Duration.ofSeconds(10))
                        .register(meterRegistry);
    }

    public void recordCheckout(Duration duration) {
        checkoutTimer.record(duration);
    }

    public Timer getTimer() {
        return checkoutTimer;
    }
}
