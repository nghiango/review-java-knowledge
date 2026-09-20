package lab.databasesql.questions;

public class Q09IsolationAnomalies {

    public static void main(String[] args) {
        // Isolation Levels & Anomalies:
        // READ UNCOMMITTED: Dirty Reads (PostgreSQL treats this as READ COMMITTED).
        // READ COMMITTED (Default in PG): Prevents Dirty Reads. Subject to Non-Repeatable Reads &
        // Phantoms.
        // REPEATABLE READ: Prevents Dirty, Non-Repeatable, and Phantom Reads in PG (via MVCC
        // snapshot at transaction start). Subject to Write Skew.
        // SERIALIZABLE: Prevents all anomalies including Write Skew (using SSI - Serializable
        // Snapshot Isolation).

        boolean readCommittedPreventsDirtyRead = true; // true
        boolean repeatableReadPreventsPhantomInPostgres = true; // true
        boolean serializablePreventsWriteSkew = true; // true

        System.out.println(
                "READ COMMITTED prevents dirty read: "
                        + readCommittedPreventsDirtyRead); // READ COMMITTED prevents dirty read:
        // true
        System.out.println(
                "REPEATABLE READ prevents phantoms in PG: "
                        + repeatableReadPreventsPhantomInPostgres); // REPEATABLE READ prevents
        // phantoms in PG: true
        System.out.println(
                "SERIALIZABLE prevents write skew: "
                        + serializablePreventsWriteSkew); // SERIALIZABLE prevents write skew: true
    }
}
