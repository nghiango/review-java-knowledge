package lab.databasesql.questions;

public class Q15OffsetVsKeysetPagination {

    public static void main(String[] args) {
        // OFFSET Pagination:
        // Query: SELECT * FROM items ORDER BY id LIMIT 20 OFFSET 500000;
        // Engine scans 500,020 rows off disk/cache and discards 500,000 -> O(N) complexity.
        // Fails with duplicate items / missed rows during concurrent inserts.
        boolean offsetHasLinearDegradation = true; // true

        // Keyset / Cursor Pagination:
        // Query: SELECT * FROM items WHERE id > 500000 ORDER BY id LIMIT 20;
        // Engine seeks the B-tree leaf node directly in O(log N) -> O(1) response time regardless
        // of table size.
        // Immune to pagination drift.
        boolean keysetHasConstantResponseTime = true; // true

        System.out.println(
                "OFFSET degrades linearly: "
                        + offsetHasLinearDegradation); // OFFSET degrades linearly: true
        System.out.println(
                "Keyset provides constant O(1) time: "
                        + keysetHasConstantResponseTime); // Keyset provides constant O(1) time:
        // true
    }
}
