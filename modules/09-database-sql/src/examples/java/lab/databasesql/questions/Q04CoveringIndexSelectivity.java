package lab.databasesql.questions;

public class Q04CoveringIndexSelectivity {

    public static void main(String[] args) {
        // High Selectivity: Query returns a tiny percentage of rows (e.g. 0.01%) -> Index Scan is
        // chosen by query planner.
        // Low Selectivity: Query matches 40% of rows -> Sequential Scan is faster than random page
        // fetches.
        double highSelectivityRatio = 0.0001; // 0.0001
        boolean prefersIndexScanOnHighSelectivity = (highSelectivityRatio < 0.05); // true

        // Covering Index / Index-Only Scan:
        // CREATE INDEX idx_user ON users(email) INCLUDE (username, status);
        // All projected and filtered columns exist in the index leaf pages, skipping Heap table
        // access entirely!
        boolean indexOnlyScanBypassesHeapTableAccess = true; // true

        System.out.println(
                "Prefers Index Scan on high selectivity: "
                        + prefersIndexScanOnHighSelectivity); // Prefers Index Scan on high
        // selectivity: true
        System.out.println(
                "Covering index enables Index-Only Scan: "
                        + indexOnlyScanBypassesHeapTableAccess); // Covering index enables
        // Index-Only Scan: true
    }
}
