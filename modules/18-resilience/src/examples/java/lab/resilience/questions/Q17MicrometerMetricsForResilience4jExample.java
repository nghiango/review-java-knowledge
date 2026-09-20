package lab.resilience.questions;

import java.util.Map;

public class Q17MicrometerMetricsForResilience4jExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Standard Resilience4j Micrometer metric names:
        // resilience4j.circuitbreaker.state (tag: state=closed|open|half_open)
        // resilience4j.circuitbreaker.calls (tag: kind=successful|failed|not_permitted)
        // resilience4j.retry.calls (tag:
        // kind=successful_without_retry|successful_with_retry|failed_with_retry)
        Map<String, String> cbStateTags =
                Map.of(
                        "name", "orderClient",
                        "state", "closed");

        boolean tracksState = "closed".equals(cbStateTags.get("state")); // true
        boolean hasServiceName = "orderClient".equals(cbStateTags.get("name")); // true

        System.out.println(
                "Metric tagged correctly for Prometheus: " + (tracksState && hasServiceName));
    }
}
