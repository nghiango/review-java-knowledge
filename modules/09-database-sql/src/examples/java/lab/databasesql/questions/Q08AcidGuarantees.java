package lab.databasesql.questions;

public class Q08AcidGuarantees {

    public static void main(String[] args) {
        // ACID properties implemented by PostgreSQL:
        // Atomicity: All or nothing (via Undo/Redo WAL logging and transaction commit markers).
        // Consistency: Schema constraints, foreign keys, and invariants always preserved.
        // Isolation: Concurrent transactions executed without seeing intermediate uncommitted state
        // (MVCC + locks).
        // Durability: Committed transactions survive crash/power loss (via Write-Ahead Log WAL
        // fsync to persistent storage).

        boolean walFsyncEnsuresDurability = true; // true
        boolean mvccProvidesTransactionIsolation = true; // true

        System.out.println(
                "WAL fsync guarantees durability: "
                        + walFsyncEnsuresDurability); // WAL fsync guarantees durability: true
        System.out.println(
                "MVCC provides transaction isolation: "
                        + mvccProvidesTransactionIsolation); // MVCC provides transaction isolation:
        // true
    }
}
