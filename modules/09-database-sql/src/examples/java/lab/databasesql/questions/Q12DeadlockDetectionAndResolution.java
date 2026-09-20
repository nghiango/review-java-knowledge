package lab.databasesql.questions;

public class Q12DeadlockDetectionAndResolution {

    public static void main(String[] args) {
        // PostgreSQL Deadlock Detection:
        // Configured by deadlock_timeout (default: 1000ms = 1s).
        // If a transaction waits > 1s for a lock, PostgreSQL builds the Wait-For Graph, detects
        // cycles, and aborts one transaction with SQLSTATE 40P01.
        int defaultDeadlockTimeoutMs = 1000; // 1000

        // Prevention Strategy: Canonical Resource Ordering
        // When locking multiple rows (e.g. Account A and Account B), always acquire locks in sorted
        // primary key order (e.g. min(id1, id2) then max(id1, id2)).
        // This eliminates the circular wait condition (Coffman condition 4).
        boolean canonicalOrderingPreventsDeadlocks = true; // true

        System.out.println(
                "Default deadlock timeout ms: "
                        + defaultDeadlockTimeoutMs); // Default deadlock timeout ms: 1000
        System.out.println(
                "Canonical ordering prevents deadlocks: "
                        + canonicalOrderingPreventsDeadlocks); // Canonical ordering prevents
        // deadlocks: true
    }
}
