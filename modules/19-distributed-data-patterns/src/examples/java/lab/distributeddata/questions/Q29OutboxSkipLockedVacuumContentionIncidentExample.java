package lab.distributeddata.questions;

import java.util.Map;

/**
 * Q29: Production Incident: Outbox polling query stalled PostgreSQL autovacuum, causing transaction ID wraparound emergency.
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q29OutboxSkipLockedVacuumContentionIncidentExample {

    public static void main(String[] args) {
        // Incident Scenario:
        // A polling outbox publisher executed:
        // SELECT * FROM outbox_table WHERE status = 'PENDING' ORDER BY id LIMIT 500 FOR UPDATE SKIP LOCKED
        // Once published, records were immediately marked as 'PROCESSED' or deleted via DELETE FROM outbox_table.
        // At 10,000 events/sec, the table accumulated 500 million dead tuples per week.

        // Failure Mechanism:
        // 1. Long-running outbox polling transactions and uncommitted workers prevented PostgreSQL autovacuum
        //    from advancing the database transaction ID horizon (datfrozenxid).
        // 2. Table and index bloat grew from 50MB to 180GB.
        // 3. PostgreSQL entered emergency read-only mode to prevent transaction ID wraparound (TXID wraparound outage)!

        boolean unvacuumedDeadTuplesHaltDatabase = true; // true

        // Remediation:
        // 1. Partition the outbox table by time (e.g. daily/hourly partitions) and DROP TABLE partitions
        //    instead of issuing millions of row-level DELETE statements (instant space reclaim, 0 dead tuples).
        // 2. Or switch from Polling Publisher to Log-Based Change Data Capture (Debezium reading PostgreSQL WAL),
        //    which requires zero row polling and zero lock contention.

        Map<String, String> permanentFixes =
                Map.of(
                        "Partition Dropping", "DROP TABLE outbox_p2026_09; zero autovacuum load; instant space reclaim",
                        "Debezium CDC", "Reads write-ahead log directly; zero polling queries on database tables");

        boolean avoidsAutovacuumOverhead =
                permanentFixes.containsKey("Debezium CDC"); // true
    }
}
