package lab.databasesql.questions;

public class Q20CommonTableExpressions {

    public static void main(String[] args) {
        // Common Table Expressions (WITH clause):
        // 1. Non-recursive CTE: Improves query readability and modular structure.
        // In PostgreSQL 12+, non-recursive CTEs are inlined by default unless declared WITH ... AS
        // MATERIALIZED.
        boolean cteInlinedByDefaultInModernPostgres = true; // true

        // 2. Recursive CTE (WITH RECURSIVE): Enables graph traversal, hierarchical organizational
        // charts, and tree path resolution directly in SQL.
        boolean recursiveCteSupportsHierarchicalTrees = true; // true

        System.out.println(
                "Modern Postgres inlines standard CTEs: "
                        + cteInlinedByDefaultInModernPostgres); // Modern Postgres inlines standard
        // CTEs: true
        System.out.println(
                "Recursive CTE supports tree hierarchies: "
                        + recursiveCteSupportsHierarchicalTrees); // Recursive CTE supports tree
        // hierarchies: true
    }
}
