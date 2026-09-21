# Solution: Carrier Pinning Circuit Breaker

## Categorized Issues

1. `// Performance issue: Synchronized method wrapping the entire supplier serializes all downstream executions to concurrency level 1.`
2. `// Concurrency issue: Holding monitor locks during long-running remote I/O creates carrier thread starvation and stalls virtual thread scheduling pools.`
3. `// Reliability issue: Missing timeout-based half-open recovery state transition leaves the circuit breaker permanently locked in OPEN state after tripping.`

## Annotated Code

```java
package lab.java25boot4.resilience.broken.pinningbreaker;

import java.util.function.Supplier;

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

    // Performance issue: Synchronized method wrapping the entire supplier serializes all downstream executions to concurrency level 1.
    // Concurrency issue: Holding monitor locks during long-running remote I/O creates carrier thread starvation and stalls virtual thread scheduling pools.
    public synchronized <T> T execute(Supplier<T> action) {
        // Reliability issue: Missing timeout-based half-open recovery state transition leaves the circuit breaker permanently locked in OPEN state after tripping.
        if (state == State.OPEN) {
            throw new IllegalStateException("Circuit breaker is OPEN");
        }

        try {
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
```

## Correct Implementation
The correct production implementation uses non-blocking atomic references (`AtomicReference<CircuitState>`) or lightweight lock-free state machines. The circuit state check occurs before invoking the supplier, and recording success/failure occurs after execution without holding any monitor locks across network I/O.
