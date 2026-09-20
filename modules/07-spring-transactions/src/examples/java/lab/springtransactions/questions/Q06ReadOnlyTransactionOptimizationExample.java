package lab.springtransactions.questions;

public class Q06ReadOnlyTransactionOptimizationExample {

    record ReadOnlySettings(
            boolean isReadOnly,
            boolean hibernateFlushingDisabled,
            boolean dbEngineSnapshotOptimization) {}

    public static void main(String[] args) {
        // @Transactional(readOnly = true) enables performance optimizations:
        // 1. Hibernate sets FlushMode.MANUAL (skips dirty checking at commit)
        // 2. JDBC Connection.setReadOnly(true) allows DB replicas to route queries
        ReadOnlySettings settings = new ReadOnlySettings(true, true, true);

        boolean isReadOnly = settings.isReadOnly(); // true
        boolean skipsDirtyCheck = settings.hibernateFlushingDisabled(); // true
        boolean engineOptimized = settings.dbEngineSnapshotOptimization(); // true

        System.out.println(
                "ReadOnly: "
                        + isReadOnly
                        + ", Skips dirty check: "
                        + skipsDirtyCheck
                        + ", Engine optimized: "
                        + engineOptimized);
    }
}
