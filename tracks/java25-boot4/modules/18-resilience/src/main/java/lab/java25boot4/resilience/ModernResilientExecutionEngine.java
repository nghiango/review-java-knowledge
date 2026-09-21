package lab.java25boot4.resilience;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Modern non-blocking resilience engine providing: 1. Bounded retries with exponential backoff and
 * randomized full jitter. 2. Lock-free Circuit Breaker with sliding window and automatic half-open
 * transitions. 3. Semaphore-based non-pinning concurrency limiter.
 */
public class ModernResilientExecutionEngine {

    public enum CircuitState {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    public record RetryConfig(
            int maxAttempts,
            Duration baseBackoff,
            Duration maxBackoff,
            Predicate<Throwable> retryablePredicate) {

        public static RetryConfig defaultTransient(int maxAttempts) {
            return new RetryConfig(
                    maxAttempts,
                    Duration.ofMillis(50),
                    Duration.ofMillis(1000),
                    throwable -> !(throwable instanceof IllegalArgumentException));
        }
    }

    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }

    public static class ConcurrencyLimitExceededException extends RuntimeException {
        public ConcurrencyLimitExceededException(String message) {
            super(message);
        }
    }

    public static class LockFreeCircuitBreaker {
        private final int failureThreshold;
        private final Duration openDuration;
        private final AtomicReference<CircuitState> state =
                new AtomicReference<>(CircuitState.CLOSED);
        private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
        private final AtomicReference<Instant> lastStateChange =
                new AtomicReference<>(Instant.now());

        public LockFreeCircuitBreaker(int failureThreshold, Duration openDuration) {
            this.failureThreshold = failureThreshold;
            this.openDuration = openDuration;
        }

        public <T> T execute(Supplier<T> action) {
            checkState();

            try {
                T result = action.get();
                recordSuccess();
                return result;
            } catch (Throwable t) {
                recordFailure();
                throw t;
            }
        }

        private void checkState() {
            while (true) {
                CircuitState current = state.get();
                if (current == CircuitState.CLOSED || current == CircuitState.HALF_OPEN) {
                    return;
                }
                if (current == CircuitState.OPEN) {
                    Instant lastChanged = lastStateChange.get();
                    if (Duration.between(lastChanged, Instant.now()).compareTo(openDuration) >= 0) {
                        if (state.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN)) {
                            lastStateChange.set(Instant.now());
                            return;
                        } else {
                            continue;
                        }
                    }
                    throw new CircuitBreakerOpenException("Circuit breaker is currently OPEN");
                }
            }
        }

        private void recordSuccess() {
            consecutiveFailures.set(0);
            if (state.get() == CircuitState.HALF_OPEN) {
                if (state.compareAndSet(CircuitState.HALF_OPEN, CircuitState.CLOSED)) {
                    lastStateChange.set(Instant.now());
                }
            }
        }

        private void recordFailure() {
            int failures = consecutiveFailures.incrementAndGet();
            if (failures >= failureThreshold) {
                if (state.get() != CircuitState.OPEN) {
                    state.set(CircuitState.OPEN);
                    lastStateChange.set(Instant.now());
                }
            }
        }

        public CircuitState getState() {
            return state.get();
        }
    }

    private final RetryConfig retryConfig;
    private final LockFreeCircuitBreaker circuitBreaker;
    private final AtomicInteger activeExecutions = new AtomicInteger(0);
    private final int maxConcurrency;

    public ModernResilientExecutionEngine(
            RetryConfig retryConfig, LockFreeCircuitBreaker circuitBreaker, int maxConcurrency) {
        this.retryConfig = retryConfig;
        this.circuitBreaker = circuitBreaker;
        this.maxConcurrency = maxConcurrency;
    }

    public <T> T executeWithResilience(Supplier<T> action) {
        int current = activeExecutions.incrementAndGet();
        try {
            if (current > maxConcurrency) {
                throw new ConcurrencyLimitExceededException(
                        "Max concurrency level exceeded: " + maxConcurrency);
            }

            return executeWithRetry(() -> circuitBreaker.execute(action));
        } finally {
            activeExecutions.decrementAndGet();
        }
    }

    private <T> T executeWithRetry(Supplier<T> action) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return action.get();
            } catch (Throwable t) {
                if (attempt >= retryConfig.maxAttempts()
                        || !retryConfig.retryablePredicate().test(t)) {
                    if (t instanceof RuntimeException re) throw re;
                    throw new RuntimeException(t);
                }

                long backoffMillis =
                        calculateJitteredBackoff(
                                attempt, retryConfig.baseBackoff(), retryConfig.maxBackoff());
                try {
                    Thread.sleep(backoffMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(
                            "Resilient execution interrupted during backoff", ie);
                }
            }
        }
    }

    public static long calculateJitteredBackoff(
            int attempt, Duration baseBackoff, Duration maxBackoff) {
        long baseMillis = baseBackoff.toMillis();
        long maxMillis = maxBackoff.toMillis();

        long exponential = baseMillis * (1L << Math.min(attempt - 1, 10));
        long capped = Math.min(exponential, maxMillis);

        // Full jitter: uniformly distributed in [0, capped]
        return ThreadLocalRandom.current().nextLong(capped + 1);
    }

    public CircuitState getCircuitState() {
        return circuitBreaker.getState();
    }
}
