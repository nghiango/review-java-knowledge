package lab.testing.questions;

import java.util.List;

/**
 * Q25: How does @Transactional test rollback differ from explicit database cleanup (truncation),
 * and why does @Transactional mask production commit and connection leaks?
 */
public class Q25TransactionalRollbackVsTruncationCleanup {

    public static void main(String[] args) {
        // Mode 1: @Transactional rollback:
        // - Fast in-memory rollback per test method
        // - Hazard 1: Never executes actual DB commit, masking dirty checking / flush errors
        // - Hazard 2: Doesn't work for multi-threaded tests, WebTestClient, or async workers
        boolean transactionalRollbackMasksFlush = true; // true (SQL flush errors never triggered)

        // Mode 2: Explicit Database Truncation (clean database before/after test):
        // - Real transaction commits execute, testing real PostgreSQL constraints & triggers
        // - Works across asynchronous worker threads and real HTTP request dispatches
        List<String> tablesToTruncate = List.of("orders", "order_items", "payments");
        String truncateSql =
                "TRUNCATE TABLE " + String.join(", ", tablesToTruncate) + " RESTART IDENTITY CASCADE";

        boolean realCommitsTested = !transactionalRollbackMasksFlush; // false under @Transactional

        System.out.println("Rollback Masks Flush Errors: " + transactionalRollbackMasksFlush); // true
        System.out.println("Truncate Cleanup SQL: " + truncateSql);
        System.out.println("Real Commits Tested in @Transactional: " + realCommitsTested); // false
    }
}
