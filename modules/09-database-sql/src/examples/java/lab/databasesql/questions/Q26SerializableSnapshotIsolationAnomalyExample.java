package lab.databasesql.questions;

@SuppressWarnings("unused")
public final class Q26SerializableSnapshotIsolationAnomalyExample {
    private Q26SerializableSnapshotIsolationAnomalyExample() {}

    // Write Skew Anomaly Demonstration:
    // Constraint: At least 1 doctor must be on-call (COUNT >= 1)
    // Under REPEATABLE READ:
    // Tx 1: SELECT COUNT(*) FROM on_call WHERE active = true; (returns 2: Dr. A and Dr. B)
    // Tx 2: SELECT COUNT(*) FROM on_call WHERE active = true; (returns 2)
    // Tx 1: UPDATE on_call SET active = false WHERE doctor = 'A'; (commits)
    // Tx 2: UPDATE on_call SET active = false WHERE doctor = 'B'; (commits!)
    // Both commits succeed under Repeatable Read because they update disjoint rows,
    // but the business invariant is violated: 0 doctors remain on call!
    public static class WriteSkewSimulator {
        public static String explainSsiProtection() {
            return "PostgreSQL Serializable Snapshot Isolation (SSI) tracks rw-antidependencies via SIREAD locks; "
                + "detects dangerous cycle in dependency graph and aborts one transaction with SQLSTATE 40001.";
        }
    }

    public static void main(String[] args) {
        String explanation = WriteSkewSimulator.explainSsiProtection();
        boolean detectsCycle = explanation.contains("40001"); // true (serialization failure triggers application retry)
    }
}
