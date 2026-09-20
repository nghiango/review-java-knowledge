package lab.databasesql.questions;

public class Q03CompositeIndexLeftmostRule {

    public static void main(String[] args) {
        // Given composite index: (colA, colB, colC)
        // Supported queries:
        // WHERE colA = ?                    -> Uses Index (leftmost 1 column)
        // WHERE colA = ? AND colB = ?        -> Uses Index (leftmost 2 columns)
        // WHERE colA = ? AND colB = ? AND colC = ? -> Uses Index (all 3 columns)

        // Unsupported direct seeks:
        // WHERE colB = ?                    -> Cannot direct seek (colA missing)
        // WHERE colC = ?                    -> Cannot direct seek (colA and colB missing)
        boolean canSeekOnColBWithoutColA = false; // false
        boolean canSeekOnColAWithColB = true; // true

        System.out.println(
                "Can seek on colB without colA: "
                        + canSeekOnColBWithoutColA); // Can seek on colB without colA: false
        System.out.println(
                "Can seek on colA + colB: "
                        + canSeekOnColAWithColB); // Can seek on colA + colB: true
    }
}
