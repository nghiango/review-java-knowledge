package lab.observability.latencyhistogram;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CorrectCheckoutLatencyTrackerTest {

    private MeterRegistry meterRegistry;
    private CorrectCheckoutLatencyTracker tracker;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        tracker = new CorrectCheckoutLatencyTracker(meterRegistry);
    }

    @Test
    @DisplayName("Should record latency distributions in Timer with count and max metrics")
    void recordCheckout_recordsDurationHistogram() {
        tracker.recordCheckout(Duration.ofMillis(45));
        tracker.recordCheckout(Duration.ofMillis(95));
        tracker.recordCheckout(Duration.ofMillis(850));

        Timer timer = tracker.getTimer();
        assertThat(timer.count()).isEqualTo(3L);
        assertThat(timer.max(java.util.concurrent.TimeUnit.MILLISECONDS))
                .isGreaterThanOrEqualTo(850.0);
    }
}
