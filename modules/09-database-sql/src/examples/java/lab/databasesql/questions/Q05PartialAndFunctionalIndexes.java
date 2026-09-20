package lab.databasesql.questions;

public class Q05PartialAndFunctionalIndexes {

    public static void main(String[] args) {
        // Partial Index: Indexes only a subset of table rows matching a predicate:
        // CREATE INDEX idx_unprocessed_orders ON orders(created_at) WHERE status = 'PENDING';
        // Saves memory and disk space by omitting 99% of historical 'COMPLETED' orders.
        boolean partialIndexReducesIndexFootprint = true; // true

        // Functional / Expression Index: Indexes the computed result of an expression:
        // CREATE INDEX idx_users_lower_email ON users(LOWER(email));
        // Supports fast lookups on WHERE LOWER(email) = :email
        boolean expressionIndexSupportsCaseInsensitiveLookups = true; // true

        System.out.println(
                "Partial index reduces footprint: "
                        + partialIndexReducesIndexFootprint); // Partial index reduces footprint:
        // true
        System.out.println(
                "Expression index supports expressions in WHERE: "
                        + expressionIndexSupportsCaseInsensitiveLookups); // Expression index
        // supports expressions in
        // WHERE: true
    }
}
