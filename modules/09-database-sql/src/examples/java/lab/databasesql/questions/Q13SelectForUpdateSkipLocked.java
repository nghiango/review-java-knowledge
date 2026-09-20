package lab.databasesql.questions;

public class Q13SelectForUpdateSkipLocked {

    public static void main(String[] args) {
        // SELECT ... FOR UPDATE: Blocks on locked rows until previous transaction commits or rolls
        // back.
        // SELECT ... FOR UPDATE NOWAIT: Immediately throws error (55P03: lock_not_available) if row
        // is already locked.
        // SELECT ... FOR UPDATE SKIP LOCKED: Automatically skips currently locked rows and fetches
        // the next available unlocked rows!

        // High-Throughput Job Queue Pattern:
        // SELECT id FROM job_queue WHERE status = 'PENDING' ORDER BY priority DESC LIMIT 10 FOR
        // UPDATE SKIP LOCKED;
        // Allows hundreds of concurrent worker threads to pull distinct jobs simultaneously with
        // zero lock contention!
        boolean skipLockedEnablesLockFreeQueuesInSql = true; // true

        System.out.println(
                "SKIP LOCKED enables lock-free queues in SQL: "
                        + skipLockedEnablesLockFreeQueuesInSql); // SKIP LOCKED enables lock-free
        // queues in SQL: true
    }
}
