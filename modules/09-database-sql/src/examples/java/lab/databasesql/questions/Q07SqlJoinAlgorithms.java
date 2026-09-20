package lab.databasesql.questions;

public class Q07SqlJoinAlgorithms {

    public static void main(String[] args) {
        // Nested Loop Join: For each outer row, seeks matching inner rows via index. Ideal for
        // small datasets / indexed lookups.
        // Hash Join: Builds in-memory hash table of smaller relation, probes larger relation. Ideal
        // for large unindexed equality joins.
        // Merge Join: Sorts both relations on join key, scans sequentially. Ideal when both inputs
        // are pre-sorted by index.

        boolean nestedLoopIdealForSmallIndexedInputs = true; // true
        boolean hashJoinRequiresEqualityOperator = true; // true
        boolean mergeJoinRequiresSortedInputs = true; // true

        System.out.println(
                "Nested loop ideal for indexed lookups: "
                        + nestedLoopIdealForSmallIndexedInputs); // Nested loop ideal for indexed
        // lookups: true
        System.out.println(
                "Hash join requires equality: "
                        + hashJoinRequiresEqualityOperator); // Hash join requires equality: true
        System.out.println(
                "Merge join requires pre-sorted inputs: "
                        + mergeJoinRequiresSortedInputs); // Merge join requires pre-sorted inputs:
        // true
    }
}
