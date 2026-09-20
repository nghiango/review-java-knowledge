package lab.observability.questions;

public class Q01ThreePillarsOfObservabilityExample {

    record ObservabilityPillar(String name, String dataModel, String primaryLimitation) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // The three pillars of observability:
        // 1. Logs: Discrete timestamped text/JSON events (rich context; expensive storage at
        // scale).
        // 2. Metrics: Aggregable numeric time-series data (cheap storage; no individual request
        // context).
        // 3. Traces: Request journey across distributed microservices (end-to-end causality;
        // sampling required).
        ObservabilityPillar logs =
                new ObservabilityPillar(
                        "Logs",
                        "Discrete structured events",
                        "High ingestion volume & storage cost");
        ObservabilityPillar metrics =
                new ObservabilityPillar(
                        "Metrics",
                        "Numeric time-series counters & gauges",
                        "No individual payload or request context");
        ObservabilityPillar traces =
                new ObservabilityPillar(
                        "Traces",
                        "Directed acyclic graph of spans",
                        "Network and head/tail sampling overhead");

        int pillarCount = 3; // 3
        boolean metricsAreAggregable = metrics.dataModel().contains("Numeric"); // true

        System.out.println("Total observability pillars: " + pillarCount);
        System.out.println("Metrics represent numeric data: " + metricsAreAggregable);
    }
}
