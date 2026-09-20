package lab.observability.questions;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public class Q12MetricCardinalityPreventionExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // High-cardinality values (UUIDs, email addresses, order IDs) create a new Meter per value.
        // Cardinality must be strictly bounded by using static categorical dimensions.
        MeterRegistry registry = new SimpleMeterRegistry();

        // Safe bounded dimension: 3 payment types x 2 statuses = 6 total time series
        registry.counter("payments.total", "type", "CARD", "status", "SUCCESS").increment();
        registry.counter("payments.total", "type", "PAYPAL", "status", "SUCCESS").increment();

        int registeredMeters = registry.getMeters().size(); // 2
        boolean isSafeBoundedSize = registeredMeters < 100; // true

        System.out.println("Total registered metric time series: " + registeredMeters);
        System.out.println("Metric registry memory is bounded: " + isSafeBoundedSize);
    }
}
