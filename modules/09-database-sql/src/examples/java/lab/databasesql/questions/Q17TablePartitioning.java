package lab.databasesql.questions;

public class Q17TablePartitioning {

    public static void main(String[] args) {
        // Table Partitioning Strategies in PostgreSQL:
        // 1. Range Partitioning: By date ranges (e.g. logs_2026_01, logs_2026_02)
        // 2. List Partitioning: By discrete categories (e.g. region_us, region_eu)
        // 3. Hash Partitioning: By hash modulus (even distribution across N shards)

        // Partition Pruning: The query planner ignores irrelevant child tables at query plan or
        // execution time
        // (e.g. WHERE created_at BETWEEN '2026-01-01' AND '2026-01-31' scans ONLY logs_2026_01).
        boolean partitionPruningSkipsIrrelevantPartitions = true; // true

        // Dropping old partitions (DROP TABLE logs_2020_01) is an instantaneous O(1) metadata
        // operation,
        // unlike slow DELETE queries that generate massive WAL and dead tuples!
        boolean droppingPartitionIsInstantMetadataOperation = true; // true

        System.out.println(
                "Partition pruning skips unneeded partitions: "
                        + partitionPruningSkipsIrrelevantPartitions); // Partition pruning skips
        // unneeded partitions: true
        System.out.println(
                "Dropping partition is instant: "
                        + droppingPartitionIsInstantMetadataOperation); // Dropping partition is
        // instant: true
    }
}
