package lab.resilience.questions;

public class Q23IncidentCircuitBreakerStuckOpenStateExample {

    record CircuitDiagnosis(
            int waitDurationSeconds, int permittedCallsInHalfOpen, boolean probeFailed) {
        boolean isStuckOpenDueToPermittedZero() {
            return permittedCallsInHalfOpen == 0;
        }
    }

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // If permittedNumberOfCallsInHalfOpenState is misconfigured or probe endpoints are
        // improperly filtered,
        // the circuit breaker cannot successfully transition from HALF_OPEN back to CLOSED.
        CircuitDiagnosis healthyConfig = new CircuitDiagnosis(10, 5, false);
        CircuitDiagnosis brokenConfig = new CircuitDiagnosis(10, 0, false);

        boolean isMisconfigured = brokenConfig.isStuckOpenDueToPermittedZero(); // true
        boolean isOperatingNormally = !healthyConfig.isStuckOpenDueToPermittedZero(); // true

        System.out.println(
                "Healthy circuit probes: "
                        + isOperatingNormally
                        + ", Broken config stuck: "
                        + isMisconfigured);
    }
}
