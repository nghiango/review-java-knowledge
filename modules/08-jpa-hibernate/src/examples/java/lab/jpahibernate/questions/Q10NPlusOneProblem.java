package lab.jpahibernate.questions;

public class Q10NPlusOneProblem {

    public static void main(String[] args) {
        int rootEntitiesCount = 100;
        // Naive iteration over lazy association triggers: 1 initial query + N individual queries
        int naiveTotalQueries = 1 + rootEntitiesCount; // 101

        // With JOIN FETCH or @EntityGraph: single query joins parent and child tables
        int joinFetchQueries = 1; // 1

        // With @BatchSize(size = 25): 1 initial query + ceil(100 / 25) = 4 queries
        int batchSizeQueries = 1 + (rootEntitiesCount / 25); // 5

        System.out.println(
                "Naive queries for 100 entities: "
                        + naiveTotalQueries); // Naive queries for 100 entities: 101
        System.out.println("JOIN FETCH queries: " + joinFetchQueries); // JOIN FETCH queries: 1
        System.out.println(
                "BatchSize(25) queries: " + batchSizeQueries); // BatchSize(25) queries: 5
    }
}
