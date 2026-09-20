package lab.resilience.questions;

import java.util.concurrent.Semaphore;

public class Q18VirtualThreadsImpactOnBulkheadsExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // With Project Loom Virtual Threads, ThreadPool Bulkheads add unnecessary context switching
        // and thread management overhead. Semaphore Bulkhead (or Semaphore-based rate limiting)
        // is the idiomatic isolation mechanism for Virtual Threads.
        Semaphore virtualThreadBulkhead = new Semaphore(50);

        boolean permitAcquired = virtualThreadBulkhead.tryAcquire(); // true
        boolean isVirtualThreadFriendly =
                permitAcquired && (virtualThreadBulkhead.availablePermits() == 49); // true

        virtualThreadBulkhead.release();
        System.out.println("Virtual thread semaphore bulkhead active: " + isVirtualThreadFriendly);
    }
}
