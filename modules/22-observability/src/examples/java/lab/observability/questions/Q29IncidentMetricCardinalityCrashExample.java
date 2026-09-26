package lab.observability.questions;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates an incident where high cardinality metrics (e.g. raw URL paths with UUIDs or user IDs)
 * threatened to crash Prometheus and heap memory, remediated with MeterFilter.replaceTagValues or maximumAllowableTags.
 */
public class Q29IncidentMetricCardinalityCrashExample {

    public static void main(String[] args) {
        MeterRegistry registry = new SimpleMeterRegistry();

        // Remediate cardinality explosion: sanitize dynamic IDs from uri tag before registering meter
        registry.config().meterFilter(MeterFilter.replaceTagValues(
                "uri",
                val -> val.matches(".*/users/\\d+.*") ? "/api/v1/users/{id}" : val
        ));

        // Register 1000 simulated distinct user requests
        for (int i = 0; i < 1000; i++) {
            registry.counter("http.server.requests", "uri", "/api/v1/users/" + i).increment();
        }

        // Because of the filter, all 1000 requests map to exactly 1 distinct counter series
        int totalMeters = registry.getMeters().size();
        boolean cardinalityContained = totalMeters == 1; // true

        System.out.println("Total distinct metric series created: " + totalMeters); // 1
        System.out.println("High cardinality successfully contained: " + cardinalityContained); // true
    }
}
