package lab.distributeddata.questions;

import java.time.Duration;

public class Q17OutboxCleanupPartitioningAndRetentionExample {

    record RetentionStrategy(String strategy, Duration retentionPeriod, boolean avoidsTableBloat) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Continuous single-row deletes cause MVCC table bloat in PostgreSQL.
        // Production standard: Partition outbox table by day/hour and drop old partitions instantly
        // via DDL.
        RetentionStrategy tablePartitioning =
                new RetentionStrategy("PARTITION_DROP", Duration.ofDays(7), true);

        boolean preventsBloat = tablePartitioning.avoidsTableBloat(); // true
        System.out.println(
                "Partition drop eliminates PostgreSQL vacuum/bloat overhead: " + preventsBloat);
    }
}
