package lab.resilience.questions;

import java.util.Map;

/**
 * Q25: What is the role of automaticTransitionFromOpenToHalfOpenEnabled in Resilience4j?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q25CircuitBreakerAutomaticHalfOpenTransitionExample {

    public static void main(String[] args) {
        // By default: automaticTransitionFromOpenToHalfOpenEnabled = false
        // When waitDurationInOpenState expires, the circuit breaker stays in OPEN until a new
        // incoming client request arrives to trigger the state evaluation.
        //
        // Problem with low-traffic / background services:
        // If traffic ceases or upstream callers circuit-break elsewhere, Prometheus gauges report
        // the circuit breaker as OPEN indefinitely, triggering persistent false alerts.
        //
        // With automaticTransitionFromOpenToHalfOpenEnabled = true:
        // A dedicated background scheduler automatically flips the state from OPEN to HALF_OPEN
        // as soon as waitDurationInOpenState elapses, emitting a transition event without waiting
        // for incoming traffic!

        Map<String, String> transitionModes =
                Map.of(
                        "false (default)", "Lazy transition on next incoming client request",
                        "true", "Eager transition via background timer; accurate monitoring & metrics");

        boolean triggersImmediateMetricsUpdate =
                transitionModes.get("true").contains("accurate monitoring"); // true
    }
}
