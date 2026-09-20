package lab.jpahibernate.questions;

public class Q05IdGenerationStrategies {

    public static void main(String[] args) {
        // GenerationType.IDENTITY forces an immediate INSERT on persist() to retrieve the generated
        // ID from DB,
        // which disables Hibernate's write-behind JDBC batching.
        boolean identityDisablesBatching = true; // true

        // GenerationType.SEQUENCE with allocationSize (pooled optimizer) allows pre-fetching a
        // block of IDs in memory,
        // enabling full JDBC batching for INSERT statements.
        int sequenceAllocationSize = 50; // 50
        boolean sequenceEnablesBatching = true; // true

        System.out.println(
                "IDENTITY disables JDBC batching: "
                        + identityDisablesBatching); // IDENTITY disables JDBC batching: true
        System.out.println(
                "SEQUENCE allocation size: "
                        + sequenceAllocationSize); // SEQUENCE allocation size: 50
        System.out.println(
                "SEQUENCE enables JDBC batching: "
                        + sequenceEnablesBatching); // SEQUENCE enables JDBC batching: true
    }
}
