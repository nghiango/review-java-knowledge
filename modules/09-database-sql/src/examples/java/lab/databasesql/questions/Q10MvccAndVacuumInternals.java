package lab.databasesql.questions;

public class Q10MvccAndVacuumInternals {

    public static void main(String[] args) {
        // Multi-Version Concurrency Control (MVCC) in PostgreSQL:
        // Every tuple header contains xmin (inserting TX id) and xmax (deleting/updating TX id).
        // Readers never block writers, writers never block readers.
        boolean readersNeverBlockWritersInMvcc = true; // true

        // Updating a row in PostgreSQL does not overwrite in place; it inserts a new tuple version
        // and sets old tuple's xmax.
        // VACUUM (and autovacuum daemon):
        // Reclaims space occupied by dead tuples (tuples whose xmax is older than any active
        // transaction's snapshot) and updates Visibility Map / Free Space Map.
        // Prevents Transaction ID Wraparound (TXID freeze).
        boolean autovacuumReclaimsDeadTuples = true; // true

        System.out.println(
                "Readers do not block writers: "
                        + readersNeverBlockWritersInMvcc); // Readers do not block writers: true
        System.out.println(
                "Autovacuum reclaims dead tuple space: "
                        + autovacuumReclaimsDeadTuples); // Autovacuum reclaims dead tuple space:
        // true
    }
}
