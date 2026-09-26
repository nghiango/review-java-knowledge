package lab.resilience.questions;

import java.util.Map;

/**
 * Q27: How does Service Mesh Outlier Detection complement application-level Resilience4j Circuit Breakers?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q27ServiceMeshOutlierDetectionVsAppCircuitBreakerExample {

    public static void main(String[] args) {
        // Infrastructure Layer (Service Mesh - Envoy / Istio Outlier Detection):
        // Operates at L4/L7 network proxy level.
        // Evicts unhealthy POD instances from the cluster load-balancing pool when they return 5xx errors.
        // Protects against single corrupted or failing container instances across a pool of 20 pods.

        // Application Layer (Resilience4j Circuit Breaker in Spring Boot):
        // Operates inside the JVM code.
        // Understands domain business semantics (e.g. distinguishing bad customer credit card 422 vs payment gateway 503).
        // Executes localized fallback strategies (cached response, degraded mode, asynchronous outbox queue).

        Map<String, String> layerResponsibilities =
                Map.of(
                        "Envoy Outlier Detection", "Evicts bad pod IPs from load balancer pool (infrastructure health)",
                        "Resilience4j CircuitBreaker", "Domain-aware circuit breaking & business fallback execution");

        boolean layeredDefenseRecommended =
                layerResponsibilities.containsKey("Envoy Outlier Detection")
                        && layerResponsibilities.containsKey("Resilience4j CircuitBreaker"); // true
    }
}
