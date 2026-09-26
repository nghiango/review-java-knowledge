package lab.performance.questions;

/**
 * Demonstrates allocation profiling and flame graph interpretation using Async-Profiler (-e alloc),
 * differentiating between byte-weighted allocation rate (MB/s) and object count churn.
 */
public final class Q27AsyncProfilerFlameGraphAllocationProfilingExample {
    public static void main(String[] args) {
        long totalAllocatedBytes = 500L * 1024 * 1024; // 500 MB
        long measurementDurationSec = 5;
        double allocationRateMbPerSec = (double) totalAllocatedBytes / (1024 * 1024) / measurementDurationSec; // 100.0 MB/s

        boolean isHighAllocationRate = allocationRateMbPerSec >= 100.0; // true
        System.out.println("Allocation rate (MB/s): " + allocationRateMbPerSec);
        System.out.println("Flags GC pressure warning: " + isHighAllocationRate);
    }
}
