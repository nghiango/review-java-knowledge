package lab.databasesql.questions;

@SuppressWarnings("unused")
public final class Q28KeysetSeekPaginationCostExample {
    private Q28KeysetSeekPaginationCostExample() {}

    public static class PaginationComparison {
        // Flawed: OFFSET 1000000 LIMIT 20
        // Reads 1,000,020 rows from disk/index, parses their visibility, and discards 1,000,000 rows!
        // Execution time increases linearly with page depth (seconds to minutes on large tables).
        public static String generateOffsetQuery(int offset, int limit) {
            return "SELECT id, title, created_at FROM articles ORDER BY created_at DESC, id DESC LIMIT "
                + limit + " OFFSET " + offset;
        }

        // Production Keyset Pagination (Seek method):
        // Seeks directly to the composite index position via binary search in O(log N) time,
        // scanning exactly 20 index tuples regardless of whether browsing page 1 or page 50,000!
        public static String generateKeysetQuery(int limit) {
            return """
                SELECT id, title, created_at
                FROM articles
                WHERE (created_at, id) < (:last_created_at, :last_id)
                ORDER BY created_at DESC, id DESC
                LIMIT 20;
                """;
        }
    }

    public static void main(String[] args) {
        String offsetSql = PaginationComparison.generateOffsetQuery(100_000, 20);
        boolean hasOffset = offsetSql.contains("OFFSET 100000"); // true (O(N) cost)

        String keysetSql = PaginationComparison.generateKeysetQuery(20);
        boolean isConstantTime = keysetSql.contains("WHERE (created_at, id) <"); // true (O(1) seek cost)
    }
}
