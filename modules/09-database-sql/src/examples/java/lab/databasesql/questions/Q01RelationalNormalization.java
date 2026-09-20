package lab.databasesql.questions;

public class Q01RelationalNormalization {

    public static void main(String[] args) {
        // Normal Forms:
        // 1NF: Atomic values, no repeating groups
        // 2NF: 1NF + No partial functional dependencies on composite primary key
        // 3NF: 2NF + No transitive functional dependencies (non-key columns depend solely on PK)
        // BCNF: Stricter 3NF (for every functional dependency X -> Y, X is a superkey)
        boolean is3NFZeroRedundancyForNonKeys = true; // true

        // Intentional Denormalization: Trades storage and write complexity for query read
        // performance (eliminating joins)
        boolean denormalizationImprovesHeavyReadThroughput = true; // true

        System.out.println(
                "3NF eliminates non-key transitive dependency: "
                        + is3NFZeroRedundancyForNonKeys); // 3NF eliminates non-key transitive
        // dependency: true
        System.out.println(
                "Denormalization optimizes read performance: "
                        + denormalizationImprovesHeavyReadThroughput); // Denormalization optimizes
        // read performance: true
    }
}
