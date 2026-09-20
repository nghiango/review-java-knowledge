package lab.databasesql.questions;

public class Q22DatabaseBloatRemediation {

    public static void main(String[] args) {
        // Table and Index Bloat:
        // High frequency UPDATE/DELETE operations leave dead tuples that autovacuum marks as free
        // space inside 8KB pages.
        // If free space cannot be reclaimed by the OS (due to physical disk fragmentation), tables
        // and indexes grow excessively.

        // Detection: Querying pg_stat_user_tables (n_dead_tup, n_live_tup) and pgstattuple
        // extension.
        boolean deadTupleRatioMonitoredInPgStat = true; // true

        // Remediation:
        // 1. REINDEX CONCURRENTLY: Rebuilds bloated B-tree indexes online without blocking
        // concurrent INSERT/UPDATE.
        // 2. pg_repack: Reorganizes bloated tables online without holding exclusive table locks.
        boolean reindexConcurrentlyAllowsReadsAndWrites = true; // true

        System.out.println(
                "Dead tuples tracked in pg_stat_user_tables: "
                        + deadTupleRatioMonitoredInPgStat); // Dead tuples tracked in
        // pg_stat_user_tables: true
        System.out.println(
                "REINDEX CONCURRENTLY is non-blocking: "
                        + reindexConcurrentlyAllowsReadsAndWrites); // REINDEX CONCURRENTLY is
        // non-blocking: true
    }
}
