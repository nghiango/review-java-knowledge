package lab.databasesql.questions;

public class Q18ZeroDowntimeMigrations {

    public static void main(String[] args) {
        // Expand and Contract (Parallel Run) Pattern for Zero-Downtime Database Migrations:
        // Phase 1 (Expand): Add new column/table nullable. Deploy App V1.1 (writes both old and
        // new, reads old).
        // Phase 2 (Backfill): Run background worker to backfill historical data into new column.
        // Phase 3 (Switch): Deploy App V2.0 (reads from new column, writes both).
        // Phase 4 (Contract): Deploy App V2.1 (writes new only). Run migration dropping old column.

        int totalPhases = 4; // 4
        boolean eliminatesDowntimeDuringRollingDeployment = true; // true

        System.out.println(
                "Expand and Contract phases: " + totalPhases); // Expand and Contract phases: 4
        System.out.println(
                "Eliminates downtime: "
                        + eliminatesDowntimeDuringRollingDeployment); // Eliminates downtime: true
    }
}
