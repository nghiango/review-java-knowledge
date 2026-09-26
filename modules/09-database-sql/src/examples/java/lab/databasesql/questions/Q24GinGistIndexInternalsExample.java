package lab.databasesql.questions;

@SuppressWarnings("unused")
public final class Q24GinGistIndexInternalsExample {
    private Q24GinGistIndexInternalsExample() {}

    // GIN (Generalized Inverted Index) vs GiST (Generalized Search Tree) in PostgreSQL:
    // GIN: Best for JSONB containment (@>), full-text search (tsvector), and arrays where elements
    // are extracted
    // into an inverted index. Fast reads, heavier write amplification.
    // GiST: Lossy tree structure best for geometric data, range types ([1, 10]), and
    // nearest-neighbor (k-NN) queries.
    public static class IndexDesignSimulator {
        public static String generateGinIndexSql() {
            return "CREATE INDEX idx_orders_payload ON orders USING GIN (payload_jsonb jsonb_path_ops);";
        }

        public static String generateGistRangeIndexSql() {
            return "CREATE INDEX idx_reservations_period ON reservations USING GIST (booking_period);";
        }
    }

    public static void main(String[] args) {
        String ginSql = IndexDesignSimulator.generateGinIndexSql();
        boolean usesJsonbOps =
                ginSql.contains("jsonb_path_ops"); // true (compact GIN index for @> queries)

        String gistSql = IndexDesignSimulator.generateGistRangeIndexSql();
        boolean usesGist = gistSql.contains("USING GIST"); // true
    }
}
