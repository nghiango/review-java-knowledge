package lab.databasesql.questions;

public class Q23OnlineLargeTableMigration {

    public static void main(String[] args) {
        // Migrating a 100 Million Row Table with Zero Downtime:
        // Antipattern: Running `ALTER TABLE large_table ADD COLUMN status VARCHAR DEFAULT
        // 'ACTIVE';` on old PG or adding non-null columns with complex defaults.
        // Modern PostgreSQL (11+): `ADD COLUMN ... DEFAULT 'ACTIVE'` is an instant O(1)
        // metadata-only update (doesn't rewrite table).

        // Adding NOT NULL column on 100M rows without locking:
        // 1. ADD COLUMN col INT; (Instant)
        // 2. Backfill in chunks of 5000 rows (`WHERE id > :cursor AND col IS NULL LIMIT 5000;`)
        // 3. ALTER TABLE large_table ADD CONSTRAINT chk_not_null CHECK (col IS NOT NULL) NOT VALID;
        // (Instant, no lock validation)
        // 4. ALTER TABLE large_table VALIDATE CONSTRAINT chk_not_null; (Scans table without
        // blocking writes!)

        boolean validateConstraintAvoidsExclusiveLock = true; // true
        int batchBackfillChunkSize = 5000; // 5000

        System.out.println(
                "VALIDATE CONSTRAINT avoids exclusive table lock: "
                        + validateConstraintAvoidsExclusiveLock); // VALIDATE CONSTRAINT avoids
        // exclusive table lock: true
        System.out.println(
                "Recommended backfill chunk size: "
                        + batchBackfillChunkSize); // Recommended backfill chunk size: 5000
    }
}
