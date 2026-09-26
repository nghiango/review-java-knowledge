package lab.performance.questions;

/**
 * Demonstrates Linux cgroups v2 CFS quota CPU throttling diagnosis (cpu.cfs_quota_us vs cpu.cfs_period_us),
 * explaining how bursty multi-threaded tasks exhaust millisecond quotas and freeze JVM threads mid-execution.
 */
public final class Q28LinuxCgroupsV2CfsQuotaCpuThrottlingExample {
    public static void main(String[] args) {
        long cfsPeriodUs = 100_000; // 100ms period
        long cfsQuotaUs = 200_000;  // 200ms quota = 2.0 CPU cores allocated
        int activeThreads = 8;      // 8 concurrent worker threads

        // If 8 threads burn CPU simultaneously, the 200ms quota is exhausted in 25ms!
        long timeUntilThrottledMs = cfsQuotaUs / (activeThreads * 1000L); // 25ms
        boolean isThrottledBeforePeriodEnd = timeUntilThrottledMs < (cfsPeriodUs / 1000); // true

        System.out.println("Time until quota exhausted (ms): " + timeUntilThrottledMs);
        System.out.println("Throttled mid-period: " + isThrottledBeforePeriodEnd);
    }
}
