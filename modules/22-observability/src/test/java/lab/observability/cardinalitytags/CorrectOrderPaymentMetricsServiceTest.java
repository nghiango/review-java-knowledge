package lab.observability.cardinalitytags;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CorrectOrderPaymentMetricsServiceTest {

    private MeterRegistry meterRegistry;
    private CorrectOrderPaymentMetricsService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new CorrectOrderPaymentMetricsService(meterRegistry);
    }

    @Test
    @DisplayName(
            "Should increment counter using bounded low-cardinality tags without dynamic ID tags")
    void recordPaymentAttempt_usesBoundedDimensionsOnly() {
        // Record 100 payments with completely unique order IDs and user IDs
        for (int i = 0; i < 100; i++) {
            service.recordPaymentAttempt(
                    "ord-" + i, "usr-" + i, "user" + i + "@example.com", "CARD", true);
        }

        // Verify that only 1 Counter meter exists in the registry despite 100 unique orders
        assertThat(meterRegistry.getMeters()).hasSize(1);

        Counter counter =
                meterRegistry
                        .find("payments.processed.total")
                        .tag("payment_method", "CARD")
                        .tag("status", "SUCCESS")
                        .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(100.0);
    }
}
