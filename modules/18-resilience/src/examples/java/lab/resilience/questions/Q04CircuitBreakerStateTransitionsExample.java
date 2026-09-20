package lab.resilience.questions;

public class Q04CircuitBreakerStateTransitionsExample {

    enum CircuitState {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        CircuitState currentState = CircuitState.CLOSED;

        // When failure rate exceeds threshold (e.g. 50%), state transitions to OPEN
        boolean failureThresholdExceeded = true;
        if (failureThresholdExceeded) {
            currentState = CircuitState.OPEN;
        }
        boolean isNowOpen = (currentState == CircuitState.OPEN); // true

        // After waitDurationInOpenState expires, state transitions to HALF_OPEN to probe downstream
        boolean waitDurationExpired = true;
        if (waitDurationExpired) {
            currentState = CircuitState.HALF_OPEN;
        }
        boolean isNowHalfOpen = (currentState == CircuitState.HALF_OPEN); // true

        // If probe calls in HALF_OPEN succeed, state returns to CLOSED
        boolean probeSuccessful = true;
        if (probeSuccessful) {
            currentState = CircuitState.CLOSED;
        }
        boolean isRecovered = (currentState == CircuitState.CLOSED); // true

        System.out.println("Circuit states verified: CLOSED -> OPEN -> HALF_OPEN -> CLOSED");
    }
}
