package lab.resilience.questions;

import java.util.Map;

/**
 * Q26: How do Project Loom Virtual Threads change the trade-offs between ThreadPoolBulkhead and SemaphoreBulkhead?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q26ThreadPoolVsSemaphoreBulkheadVirtualThreadsExample {

    public static void main(String[] args) {
        // Platform Threads (Pre-Java 21):
        // ThreadPoolBulkhead was required for true asynchronous thread isolation and bounded queues,
        // but consumed expensive OS threads (1MB stack each) and incurred context-switching overhead.
        // SemaphoreBulkhead ran on calling thread, risking carrier thread exhaustion.

        // Project Loom Virtual Threads (Java 21+):
        // 1. ThreadPoolBulkhead is an anti-pattern with Virtual Threads: pooling virtual threads
        //    destroys the Loom design principle ("threads are cheap, never pool them").
        // 2. SemaphoreBulkhead becomes the superior, ideal bulkhead pattern:
        //    Virtual threads block on semaphores (java.util.concurrent.Semaphore) by cleanly unmounting
        //    from OS carrier threads without pinning, providing bounded concurrency with zero thread pooling overhead!

        Map<String, String> virtualThreadBulkheads =
                Map.of(
                        "ThreadPoolBulkhead", "Avoid with virtual threads; breaks Loom design principles",
                        "SemaphoreBulkhead", "Recommended with virtual threads; unmounts cleanly without thread waste");

        boolean semaphoreBulkheadIsPreferredOnLoom =
                virtualThreadBulkheads.get("SemaphoreBulkhead").contains("Recommended"); // true
    }
}
