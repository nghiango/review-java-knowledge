package lab.webflux.questions;

public class Q23IncidentFlatMapPoolAcquireTimeoutExample {

    record OutageMetrics(
            int batchSize, int defaultPrefetch, int nettyPoolLimit, boolean poolExhausted) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Incident: Batch push notification service encounters massive PoolAcquireTimeoutException.
        // Root Cause: Flux.fromIterable(batch).flatMap(client::sendNotification) with batchSize =
        // 10,000.
        // flatMap requested 256 in-flight connections per batch. With 3 concurrent batches, 768
        // requests competed
        // for Netty's default 500-channel connection pool. Pending acquire queues filled, timing
        // out after 45s.
        // Remediation:
        // 1. Enforce flatMap(client::sendNotification, 32) concurrency limit.
        // 2. Isolate inner stream errors with onErrorResume so one 429 does not abort the entire
        // batch.
        OutageMetrics metrics = new OutageMetrics(10000, 256, 500, true);

        boolean poolSaturated = (metrics.defaultPrefetch() * 3) > metrics.nettyPoolLimit(); // true
        int safeConcurrency = 32; // 32

        System.out.println("Connection pool saturated under concurrent batches: " + poolSaturated);
        System.out.println("Remediated safe concurrency bound: " + safeConcurrency);
    }
}
