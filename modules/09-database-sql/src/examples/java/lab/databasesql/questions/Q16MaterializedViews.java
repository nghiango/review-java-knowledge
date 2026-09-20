package lab.databasesql.questions;

public class Q16MaterializedViews {

    public static void main(String[] args) {
        // Standard View: Stored query definition, recomputed on every execution (no physical
        // storage).
        boolean standardViewRecomputedOnEveryQuery = true; // true

        // Materialized View: Physically stores query results on disk and supports B-tree indexes.
        // REFRESH MATERIALIZED VIEW: Recomputes data in background.
        // REFRESH MATERIALIZED VIEW CONCURRENTLY: Allows concurrent SELECTs during refresh
        // (requires UNIQUE index on view).
        boolean concurrentRefreshRequiresUniqueIndex = true; // true

        System.out.println(
                "Standard view recomputed dynamically: "
                        + standardViewRecomputedOnEveryQuery); // Standard view recomputed
        // dynamically: true
        System.out.println(
                "Concurrent refresh requires unique index: "
                        + concurrentRefreshRequiresUniqueIndex); // Concurrent refresh requires
        // unique index: true
    }
}
