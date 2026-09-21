package lab.java25boot4.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModernResilientExecutionEngineTest {

    @Test
    @DisplayName("should retry transient failures up to maximum attempts and succeed")
    void shouldRetryTransientFailuresAndSucceed() {
        AtomicInteger invocationCount = new AtomicInteger(0);

        var retryConfig =
                new ModernResilientExecutionEngine.RetryConfig(
                        3,
                        Duration.ofMillis(10),
                        Duration.ofMillis(50),
                        t -> t instanceof IllegalStateException);

        var circuitBreaker =
                new ModernResilientExecutionEngine.LockFreeCircuitBreaker(
                        5, Duration.ofMillis(200));
        var engine = new ModernResilientExecutionEngine(retryConfig, circuitBreaker, 10);

        String result =
                engine.executeWithResilience(
                        () -> {
                            if (invocationCount.incrementAndGet() < 3) {
                                throw new IllegalStateException("Transient failure");
                            }
                            return "SUCCESS";
                        });

        assertThat(result).isEqualTo("SUCCESS");
        assertThat(invocationCount.get()).isEqualTo(3);
        assertThat(engine.getCircuitState())
                .isEqualTo(ModernResilientExecutionEngine.CircuitState.CLOSED);
    }

    @Test
    @DisplayName("should not retry non-retryable exception and fail fast")
    void shouldNotRetryNonRetryableException() {
        AtomicInteger invocationCount = new AtomicInteger(0);

        var retryConfig = ModernResilientExecutionEngine.RetryConfig.defaultTransient(3);
        var circuitBreaker =
                new ModernResilientExecutionEngine.LockFreeCircuitBreaker(
                        5, Duration.ofMillis(200));
        var engine = new ModernResilientExecutionEngine(retryConfig, circuitBreaker, 10);

        assertThatThrownBy(
                        () ->
                                engine.executeWithResilience(
                                        () -> {
                                            invocationCount.incrementAndGet();
                                            throw new IllegalArgumentException("Fatal input error");
                                        }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Fatal input error");

        assertThat(invocationCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "should trip circuit breaker to OPEN state when consecutive failures reach threshold")
    void shouldTripCircuitBreakerWhenThresholdExceeded() {
        var retryConfig =
                new ModernResilientExecutionEngine.RetryConfig(
                        1, Duration.ofMillis(5), Duration.ofMillis(10), t -> false);

        var circuitBreaker =
                new ModernResilientExecutionEngine.LockFreeCircuitBreaker(3, Duration.ofMillis(50));
        var engine = new ModernResilientExecutionEngine(retryConfig, circuitBreaker, 10);

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(
                            () ->
                                    engine.executeWithResilience(
                                            () -> {
                                                throw new RuntimeException("Downstream error");
                                            }))
                    .isInstanceOf(RuntimeException.class);
        }

        assertThat(engine.getCircuitState())
                .isEqualTo(ModernResilientExecutionEngine.CircuitState.OPEN);

        // Subsequent call fails fast without executing action
        assertThatThrownBy(() -> engine.executeWithResilience(() -> "SHOULD_NOT_EXECUTE"))
                .isInstanceOf(ModernResilientExecutionEngine.CircuitBreakerOpenException.class);

        // Await half-open transition after open duration
        await().atMost(Duration.ofMillis(600))
                .pollInterval(Duration.ofMillis(10))
                .ignoreExceptionsInstanceOf(
                        ModernResilientExecutionEngine.CircuitBreakerOpenException.class)
                .untilAsserted(
                        () -> {
                            String recovered = engine.executeWithResilience(() -> "RECOVERED");
                            assertThat(recovered).isEqualTo("RECOVERED");
                            assertThat(engine.getCircuitState())
                                    .isEqualTo(ModernResilientExecutionEngine.CircuitState.CLOSED);
                        });
    }

    @Test
    @DisplayName("should calculate jittered backoff within exponential ceiling")
    void shouldCalculateJitteredBackoffWithinBounds() {
        Duration base = Duration.ofMillis(100);
        Duration max = Duration.ofMillis(800);

        for (int attempt = 1; attempt <= 5; attempt++) {
            long jitter =
                    ModernResilientExecutionEngine.calculateJitteredBackoff(attempt, base, max);
            assertThat(jitter).isGreaterThanOrEqualTo(0);
            assertThat(jitter).isLessThanOrEqualTo(800);
        }
    }
}
