package lab.databasesql.questions;

public class Q14AdvisoryLocks {

    public static void main(String[] args) {
        // PostgreSQL Advisory Locks:
        // Application-defined locks using 64-bit integer keys (pg_advisory_lock(bigint) /
        // pg_advisory_xact_lock(bigint)).
        // Scopes:
        // Session-level: pg_advisory_lock(key) -> explicit unlock required
        // (pg_advisory_unlock(key)) or released on connection close.
        // Transaction-level: pg_advisory_xact_lock(key) -> automatically released upon transaction
        // commit/rollback.

        // Used by database migration tools (Flyway, Liquibase) and distributed cron leaders without
        // requiring Redis or ZooKeeper!
        boolean migrationToolsUseAdvisoryLocks = true; // true
        boolean transactionAdvisoryLockAutoReleasesOnCommit = true; // true

        System.out.println(
                "Flyway/Liquibase use advisory locks: "
                        + migrationToolsUseAdvisoryLocks); // Flyway/Liquibase use advisory locks:
        // true
        System.out.println(
                "XACT advisory lock auto-releases on commit: "
                        + transactionAdvisoryLockAutoReleasesOnCommit); // XACT advisory lock
        // auto-releases on commit:
        // true
    }
}
