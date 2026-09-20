package lab.observability.questions;

public class Q07RedMethodMonitoringExample {

    record RedMetrics(
            double rateRequestsPerSec, double errorRatePercentage, double durationP99Ms) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // The RED Method (Tom Wilkie) applies to request-driven services (HTTP REST, gRPC,
        // GraphQL):
        // 1. Rate: Number of incoming requests per second.
        // 2. Errors: Number of failed requests per second (HTTP 5xx, exceptions).
        // 3. Duration: Distribution of time taken by requests (p50, p95, p99 latency).
        RedMetrics apiMetrics = new RedMetrics(1500.0, 0.05, 45.0);

        boolean isHealthy = apiMetrics.errorRatePercentage() < 1.0; // true
        boolean durationWithinSlo = apiMetrics.durationP99Ms() < 100.0; // true

        System.out.println("RED error rate within SLA: " + isHealthy);
        System.out.println("RED p99 duration within SLA: " + durationWithinSlo);
    }
}
