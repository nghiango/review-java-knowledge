package lab.databasesql.questions;

public class Q06ExplainAnalyzeBuffers {

    public static void main(String[] args) {
        // EXPLAIN: Estimates cost and rows based on pg_statistic planner metadata.
        // EXPLAIN ANALYZE: Actually executes the query, outputting real execution time and exact
        // row counts.
        // BUFFERS: Reports shared buffer hits (memory cache), reads (disk I/O), and dirtied pages.
        boolean explainAnalyzeExecutesTheQuery = true; // true
        boolean buffersSharedHitMeansCacheHit = true; // true

        // Node Types:
        // Seq Scan: Reads all heap pages sequentially
        // Index Scan: Seeks B-tree, then fetches corresponding Heap table pages
        // Index Only Scan: Retrieves all data directly from Index without reading Heap
        // Bitmap Heap Scan: Combines Bitmap Index Scan with batch page fetching
        String fastestScanForCoveredQuery = "Index Only Scan";

        System.out.println(
                "EXPLAIN ANALYZE executes query: "
                        + explainAnalyzeExecutesTheQuery); // EXPLAIN ANALYZE executes query: true
        System.out.println(
                "Shared hit means RAM buffer cache: "
                        + buffersSharedHitMeansCacheHit); // Shared hit means RAM buffer cache: true
        System.out.println(
                "Fastest scan type: "
                        + fastestScanForCoveredQuery); // Fastest scan type: Index Only Scan
    }
}
