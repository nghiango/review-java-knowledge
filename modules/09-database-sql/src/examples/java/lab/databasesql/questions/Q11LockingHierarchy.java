package lab.databasesql.questions;

public class Q11LockingHierarchy {

    public static void main(String[] args) {
        // PostgreSQL Locking Hierarchy:
        // 1. Row-level locks: FOR UPDATE (exclusive), FOR NO KEY UPDATE, FOR SHARE (shared), FOR
        // KEY SHARE
        // 2. Table-level locks: ACCESS SHARE (SELECT), ROW SHARE (SELECT FOR UPDATE), ROW EXCLUSIVE
        // (INSERT/UPDATE/DELETE),
        //                       SHARE ROW EXCLUSIVE, ACCESS EXCLUSIVE (ALTER TABLE / DROP TABLE)
        // 3. Page-level & Advisory locks

        // ACCESS EXCLUSIVE conflicts with all lock modes (including regular SELECT):
        boolean accessExclusiveBlocksAllQueries = true; // true
        // Regular SELECT acquires ACCESS SHARE (does not block INSERT/UPDATE):
        boolean selectDoesNotBlockRowExclusive = true; // true

        System.out.println(
                "ACCESS EXCLUSIVE blocks all concurrent queries: "
                        + accessExclusiveBlocksAllQueries); // ACCESS EXCLUSIVE blocks all
        // concurrent queries: true
        System.out.println(
                "SELECT does not block INSERT/UPDATE: "
                        + selectDoesNotBlockRowExclusive); // SELECT does not block INSERT/UPDATE:
        // true
    }
}
