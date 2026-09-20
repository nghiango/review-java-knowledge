package lab.resilience.questions;

import java.util.concurrent.Semaphore;

public class Q06BulkheadSemaphoreVsThreadPoolExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Semaphore Bulkhead: limits concurrent executions on the calling thread
        Semaphore semaphore = new Semaphore(10);
        boolean permitAcquired = semaphore.tryAcquire(); // true
        int availablePermits = semaphore.availablePermits(); // 9

        // Thread Pool Bulkhead: executes asynchronously on an isolated dedicated thread pool
        int corePoolSize = 5;
        int queueCapacity = 20;
        int maxThroughputBuffer = corePoolSize + queueCapacity; // 25

        semaphore.release();
        boolean restored = (semaphore.availablePermits() == 10); // true

        System.out.println(
                "Semaphore acquired: " + permitAcquired + ", Permits restored: " + restored);
    }
}
