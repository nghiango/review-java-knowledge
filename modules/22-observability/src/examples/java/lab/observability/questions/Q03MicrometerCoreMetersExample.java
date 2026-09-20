package lab.observability.questions;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class Q03MicrometerCoreMetersExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Core Micrometer meter types:
        // - Counter: Monotonically increasing value (requests processed, errors occurred).
        // - Timer: Measures both rate (calls/sec) and latency duration distribution.
        // - Gauge: Current instantaneous snapshot value (active connections, heap memory, queue
        // size).
        // - DistributionSummary: Measures distribution of non-time numeric values (payload byte
        // sizes).
        MeterRegistry registry = new SimpleMeterRegistry();

        Counter counter = registry.counter("orders.total");
        counter.increment();

        Timer timer = registry.timer("http.server.requests");
        timer.record(Duration.ofMillis(35));

        AtomicInteger queueSize = new AtomicInteger(14);
        registry.gauge("job.queue.depth", queueSize);

        DistributionSummary summary = registry.summary("http.response.bytes");
        summary.record(1024);

        double totalOrders = counter.count(); // 1.0
        long requestCount = timer.count(); // 1L

        System.out.println("Counter count: " + totalOrders);
        System.out.println("Timer count: " + requestCount);
    }
}
