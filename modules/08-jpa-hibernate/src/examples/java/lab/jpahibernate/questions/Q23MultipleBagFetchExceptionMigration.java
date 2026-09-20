package lab.jpahibernate.questions;

public class Q23MultipleBagFetchExceptionMigration {

    public static void main(String[] args) {
        // Attempting to JOIN FETCH two or more `List` collections (bags) simultaneously triggers:
        // org.hibernate.loader.MultipleBagFetchException: cannot simultaneously fetch multiple bags
        // Root cause: Cartesian product makes list index preservation and distinct element
        // semantics ambiguous.

        // Solution 1: Use `Set` instead of `List` (though cartesian product still inflates network
        // payload: N * M rows).
        // Solution 2 (Best practice): Split into multiple sequential queries or use @BatchSize on
        // collections.
        boolean fetchingMultipleListsInSingleQueryThrowsException = true; // true
        boolean splitQueriesEliminatesCartesianProduct = true; // true

        System.out.println(
                "Fetching multiple bags in single query throws exception: "
                        + fetchingMultipleListsInSingleQueryThrowsException); // Fetching multiple
        // bags in single
        // query throws
        // exception: true
        System.out.println(
                "Split queries eliminates cartesian product: "
                        + splitQueriesEliminatesCartesianProduct); // Split queries eliminates
        // cartesian product: true
    }
}
