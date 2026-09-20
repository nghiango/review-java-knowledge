package lab.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lab.observability.cardinalitytags.CorrectOrderPaymentMetricsService;
import lab.observability.correlationasync.CorrectAsyncOrderAuditService;
import lab.observability.latencyhistogram.CorrectCheckoutLatencyTracker;
import lab.observability.loggingsecrets.CorrectUserAuthenticationLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class ObservabilityIntegrationTest {

    private ExecutorService executor;
    private MeterRegistry meterRegistry;
    private CorrectAsyncOrderAuditService auditService;
    private CorrectOrderPaymentMetricsService metricsService;
    private CorrectCheckoutLatencyTracker latencyTracker;
    private CorrectUserAuthenticationLogger authLogger;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(4);
        meterRegistry = new SimpleMeterRegistry();
        auditService = new CorrectAsyncOrderAuditService(executor);
        metricsService = new CorrectOrderPaymentMetricsService(meterRegistry);
        latencyTracker = new CorrectCheckoutLatencyTracker(meterRegistry);
        authLogger = new CorrectUserAuthenticationLogger();
    }

    @AfterEach
    void tearDown() throws Exception {
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
        MDC.clear();
    }

    @Test
    @DisplayName(
            "End-to-end checkout flow emits metrics, logs securely, and preserves async MDC context")
    void endToEndCheckoutTelemetry_succeedsDeterministically() throws Exception {
        String orderId = "ord-999";
        String userId = "usr-123";

        // 1. Audit order placement asynchronously with MDC propagation
        MDC.put("correlationId", "corr-" + orderId);
        var auditFuture = auditService.auditOrderPlacement(orderId, userId);
        auditFuture.get(3, TimeUnit.SECONDS);

        // 2. Record payment with bounded tags
        metricsService.recordPaymentAttempt(orderId, userId, "user@test.org", "CARD", true);
        metricsService.recordPaymentAttempt("ord-1000", "usr-456", "other@test.org", "CARD", false);

        // 3. Record checkout latency
        latencyTracker.recordCheckout(Duration.ofMillis(120));

        // 4. Log sensitive operations safely
        authLogger.logLoginAttempt("alice", true, "10.0.0.1");
        authLogger.logPaymentProcessed(orderId, "4111-2222-3333-4444", 150.0);

        // Assert metrics state
        assertThat(
                        meterRegistry
                                .find("payments.processed.total")
                                .tag("status", "SUCCESS")
                                .counter()
                                .count())
                .isEqualTo(1.0);
        assertThat(
                        meterRegistry
                                .find("payments.processed.total")
                                .tag("status", "FAILED")
                                .counter()
                                .count())
                .isEqualTo(1.0);
        assertThat(latencyTracker.getTimer().count()).isEqualTo(1L);
    }
}
