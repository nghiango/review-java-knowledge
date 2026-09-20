package lab.observability.questions;

public class Q08UseMethodMonitoringExample {

    record UseMetrics(
            String resource, double utilizationPct, long saturationQueueDepth, long errorCount) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // The USE Method (Brendan Gregg) applies to infrastructure and hardware resources (CPU,
        // Memory, Disks, Network, ThreadPools):
        // 1. Utilization: Percentage of time the resource was busy performing work (e.g. CPU 75%,
        // Pool 80%).
        // 2. Saturation: Extra work waiting in queue because resource capacity is exceeded (e.g.
        // queue length > 0).
        // 3. Errors: Error count or rejected work (e.g. pool rejection count, disk read errors).
        UseMetrics dbPool = new UseMetrics("HikariCP-Main", 85.0, 0, 0);

        boolean notSaturated = dbPool.saturationQueueDepth() == 0; // true
        boolean utilizationSafe = dbPool.utilizationPct() < 90.0; // true

        System.out.println("Resource has zero saturation: " + notSaturated);
        System.out.println("Resource utilization within threshold: " + utilizationSafe);
    }
}
