package lab.databasesql.questions;

import java.util.List;

@SuppressWarnings("unused")
public final class Q27ZeroDowntimeColumnMigrationExample {
    private Q27ZeroDowntimeColumnMigrationExample() {}

    // Expand and Contract 5-phase migration sequence for renaming or transforming a database column:
    // Phase 1 (Expand): Add new column as nullable (ALTER TABLE users ADD COLUMN full_name VARCHAR(255);)
    // Phase 2 (Dual Write): Deploy application version N+1 that writes to both old and new columns, reading from old.
    // Phase 3 (Backfill): Run background worker in batches to populate historical rows from old to new column.
    // Phase 4 (Switch Read): Deploy application version N+2 that reads from new column and writes to both.
    // Phase 5 (Contract): Remove old column after verifying all instances are updated.
    public static class MigrationPhaseTracker {
        public static List<String> getPhases() {
            return List.of(
                "PHASE_1_EXPAND_ADD_COLUMN",
                "PHASE_2_DUAL_WRITE",
                "PHASE_3_BACKGROUND_BATCH_BACKFILL",
                "PHASE_4_READ_NEW_COLUMN",
                "PHASE_5_CONTRACT_DROP_OLD_COLUMN"
            );
        }
    }

    public static void main(String[] args) {
        List<String> phases = MigrationPhaseTracker.getPhases();
        int count = phases.size(); // 5
        boolean zeroDowntime = phases.contains("PHASE_2_DUAL_WRITE"); // true
    }
}
