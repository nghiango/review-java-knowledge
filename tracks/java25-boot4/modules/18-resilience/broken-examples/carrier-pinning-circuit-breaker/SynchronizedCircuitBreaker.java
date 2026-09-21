package lab.java25boot4.resilience.broken.pinningbreaker;

import java.util.function.Supplier;

/**
 * Review target: A naive circuit breaker attempting state synchronization
 * by wrapping the entire invocation and remote network call inside synchronized methods.
 */
public class SynchronizedCircuitBreaker {

    public enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    private State state = State.CLOSED;
    private int consecutiveFailures = 0;
    private final int failureThreshold;

    public SynchronizedCircuitBreaker(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public synchronized <T> T execute(Supplier<T> action) {
        if (state == State.OPEN) {
            throw new IllegalStateException("Circuit breaker is OPEN");
        }

        try {
            // Invokes arbitrary supplier (including blocking remote I/O) while holding object monitor
            T result = action.get();
            consecutiveFailures = 0;
            return result;
        } catch (Exception ex) {
            consecutiveFailures++;
            if (consecutiveFailures >= failureThreshold) {
                state = State.OPEN;
            }
            throw ex;
        }
    }

    public synchronized State getState() {
        return state;
    }
}
