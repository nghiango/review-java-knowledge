package lab.databasesql.questions;

public class Q02BTreeIndexInternals {

    public static void main(String[] args) {
        // B-Tree search time complexity is O(log N).
        // Standard PostgreSQL page size: 8 KB (8192 bytes).
        int defaultPostgresPageSizeBytes = 8192; // 8192

        // A 3-level B-tree with fan-out ~500 entries per page can index ~125 million rows in 3 I/O
        // hops.
        int treeDepth = 3; // 3
        int fanoutPerPage = 500; // 500
        long maxIndexedRowsAtDepth3 = (long) Math.pow(fanoutPerPage, treeDepth); // 125000000

        // Page splits occur when inserting into a full 8KB leaf page, dividing keys 50/50 into a
        // new page.
        boolean pageSplitIncreasesTreeDepthWhenRootSplits = true; // true

        System.out.println(
                "PostgreSQL page size: "
                        + defaultPostgresPageSizeBytes); // PostgreSQL page size: 8192
        System.out.println(
                "Max indexed rows at depth 3: "
                        + maxIndexedRowsAtDepth3); // Max indexed rows at depth 3: 125000000
        System.out.println(
                "Page split increases tree depth on root: "
                        + pageSplitIncreasesTreeDepthWhenRootSplits); // Page split increases tree
        // depth on root: true
    }
}
