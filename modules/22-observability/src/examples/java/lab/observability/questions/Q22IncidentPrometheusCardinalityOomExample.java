package lab.observability.questions;

public class Q22IncidentPrometheusCardinalityOomExample {

    record IncidentReport(
            String symptom, int registeredSeriesCount, long heapUsageMb, String remediation) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Incident: High-traffic flash sale launches; within 15 minutes, Prometheus scraper throws
        // 'context deadline exceeded' on /actuator/prometheus, and application JVM crashes with
        // OutOfMemoryError.
        // Root Cause: An engineer tagged 'orders.created.total' with customer email and dynamic
        // discount code.
        // 500,000 unique customers generated 500,000 distinct Meter instances in MeterRegistry,
        // exhausting JVM tenured heap.
        // Remediation:
        // 1. Remove customer email and promo code tags from metric meters.
        // 2. Configure MeterFilter.denyNameStartsWith() to drop rogue high-cardinality series
        // immediately.
        // 3. Move customer identifiers to structured JSON logs.
        IncidentReport report =
                new IncidentReport(
                        "Prometheus scrape timeout and JVM OutOfMemoryError: Java heap space",
                        520000,
                        3850,
                        "Drop dynamic tags via MeterFilter and log customer emails in structured logs");

        boolean cardinalityExplosionDetected = report.registeredSeriesCount() > 100000; // true
        boolean heapExhausted = report.heapUsageMb() > 3000; // true

        System.out.println("Cardinality explosion detected: " + cardinalityExplosionDetected);
        System.out.println("Heap memory saturated by meters: " + heapExhausted);
    }
}
