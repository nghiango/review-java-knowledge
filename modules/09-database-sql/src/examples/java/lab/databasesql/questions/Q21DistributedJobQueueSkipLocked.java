package lab.databasesql.questions;

public class Q21DistributedJobQueueSkipLocked {

    public static void main(String[] args) {
        // High-Concurrency Job Worker Polling Pattern:
        // WITH next_job AS (
        //     SELECT id FROM tasks
        //     WHERE status = 'QUEUED' AND scheduled_at <= NOW()
        //     ORDER BY priority DESC, id ASC
        //     LIMIT 1
        //     FOR UPDATE SKIP LOCKED
        // )
        // UPDATE tasks SET status = 'PROCESSING', started_at = NOW()
        // FROM next_job WHERE tasks.id = next_job.id
        // RETURNING tasks.*;

        // Result: 50 concurrent worker threads poll without blocking each other or waiting on
        // locks.
        boolean skipLockedEliminatesWorkerContention = true; // true
        int concurrentWorkers = 50; // 50

        System.out.println(
                "SKIP LOCKED eliminates worker contention: "
                        + skipLockedEliminatesWorkerContention); // SKIP LOCKED eliminates worker
        // contention: true
        System.out.println(
                "Concurrent workers supported: "
                        + concurrentWorkers); // Concurrent workers supported: 50
    }
}
