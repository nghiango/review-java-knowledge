package lab.springtransactions.questions;

public class Q12NestedPropagationSavepointsExample {

    record SavepointStatus(
            String savepointName, boolean rolledBackPartial, boolean outerCommitted) {}

    public static void main(String[] args) {
        // Propagation.NESTED uses JDBC Savepoints within the same physical connection
        // Inner transaction failure rolls back only to the savepoint; outer transaction can catch
        // and continue
        SavepointStatus status = new SavepointStatus("SAVEPOINT_1", true, true);

        boolean partialRollback = status.rolledBackPartial(); // true
        boolean outerSuccess = status.outerCommitted(); // true

        System.out.println(
                "Savepoint partial rollback: "
                        + partialRollback
                        + ", outer transaction committed: "
                        + outerSuccess);
    }
}
