package lab.jpahibernate.questions;

public class Q18HibernateJdbcBatching {

    public static void main(String[] args) {
        // Effective batching configuration in Hibernate:
        // hibernate.jdbc.batch_size = 50
        // hibernate.order_inserts = true
        // hibernate.order_updates = true
        // hibernate.jdbc.batch_versioned_data = true
        int configuredBatchSize = 50; // 50
        boolean orderInsertsGroupsSameTypeTogether = true; // true

        // Crucial requirement: Entity ID generator MUST NOT be GenerationType.IDENTITY!
        // (IDENTITY executes immediate INSERT and bypasses batch queue)
        String recommendedIdGenerator = "SEQUENCE";

        System.out.println(
                "Configured batch size: " + configuredBatchSize); // Configured batch size: 50
        System.out.println(
                "order_inserts groups SQL batches: "
                        + orderInsertsGroupsSameTypeTogether); // order_inserts groups SQL batches:
        // true
        System.out.println(
                "Recommended ID generator: "
                        + recommendedIdGenerator); // Recommended ID generator: SEQUENCE
    }
}
