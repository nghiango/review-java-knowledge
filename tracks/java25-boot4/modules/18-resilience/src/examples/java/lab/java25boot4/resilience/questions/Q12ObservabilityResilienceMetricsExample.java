package lab.java25boot4.resilience.questions;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Q12: How do you capture and publish Micrometer metrics and OpenTelemetry trace tags across retry
 * and circuit breaker transitions?
 */
public class Q12ObservabilityResilienceMetricsExample {

    static class ResilienceMetricsRecorder {
        private final ConcurrentHashMap<String, LongAdder> counters = new ConcurrentHashMap<>();

        public void increment(String metricName) {
            counters.computeIfAbsent(metricName, k -> new LongAdder()).increment();
        }

        public long getCount(String metricName) {
            LongAdder adder = counters.get(metricName);
            return adder != null ? adder.sum() : 0L;
        }
    }

    public static void main(String[] args) {
        var metrics = new ResilienceMetricsRecorder();

        metrics.increment("resilience.retry.attempt");
        metrics.increment("resilience.retry.attempt");
        metrics.increment("resilience.circuitbreaker.trip");

        System.out.println(
                "Retry counter: "
                        + metrics.getCount("resilience.retry.attempt")); // Retry counter: 2
        System.out.println(
                "Circuit trip counter: "
                        + metrics.getCount(
                                "resilience.circuitbreaker.trip")); // Circuit trip counter: 1
    }
}
