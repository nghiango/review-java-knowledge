package lab.performance.questions;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Demonstrates a thread pool deadlock incident where parent tasks submitted child sub-tasks to the same
 * bounded thread pool and blocked waiting for results (.get()), exhausting all worker threads and freezing the pool.
 */
public final class Q30IncidentParentChildTaskThreadPoolDeadlockExample {
    public static void main(String[] args) {
        int poolCapacity = 4;
        int activeParentTasks = 4; // All 4 worker threads hold parent tasks waiting on child futures
        int queuedChildTasks = 4;   // Child tasks wait indefinitely in queue because pool is saturated

        boolean isDeadlocked = activeParentTasks == poolCapacity && queuedChildTasks > 0; // true
        System.out.println("Thread pool starvation deadlock detected: " + isDeadlocked);
    }
}
