package lab.databasesql.questions;

@SuppressWarnings("unused")
public final class Q25CteMaterializedOptimizationFenceExample {
    private Q25CteMaterializedOptimizationFenceExample() {}

    // In PostgreSQL 12+, Common Table Expressions (CTEs) are inlined automatically (NOT
    // MATERIALIZED)
    // allowing the query planner to push outer WHERE predicates down into the CTE query.
    //
    // Specifying WITH ... AS MATERIALIZED:
    // Forces PostgreSQL to evaluate the CTE in isolation once and write results to a temporary
    // buffer,
    // acting as an intentional optimization fence to prevent expensive re-evaluations or control
    // join order.
    public static class CteOptimizationFenceSimulator {
        public static String buildInlinedCte() {
            return """
                WITH active_users AS (
                    SELECT id, email, created_at FROM users WHERE status = 'ACTIVE'
                )
                SELECT * FROM active_users WHERE created_at > NOW() - INTERVAL '7 days';
                """;
        }

        public static String buildMaterializedFenceCte() {
            return """
                WITH expensive_calc AS MATERIALIZED (
                    SELECT id, complex_metric_func(id) AS score FROM large_table
                )
                SELECT * FROM expensive_calc WHERE score > 90;
                """;
        }
    }

    public static void main(String[] args) {
        String inlined = CteOptimizationFenceSimulator.buildInlinedCte();
        boolean pushesPredicate =
                !inlined.contains("MATERIALIZED"); // true (predicate pushed down into scan)

        String fenced = CteOptimizationFenceSimulator.buildMaterializedFenceCte();
        boolean isFence = fenced.contains("AS MATERIALIZED"); // true (evaluated in isolation once)
    }
}
