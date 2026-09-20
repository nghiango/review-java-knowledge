# Database / SQL Interview Questions & Answers

<!-- --8<-- [start:basic] -->
## Basic

### 1. What are database Normal Forms (1NF, 2NF, 3NF, BCNF) and when should you intentionally denormalize?

??? question "Reveal answer"
    - **1NF:** Atomic values, no repeating groups.
    - **2NF:** 1NF + No partial dependencies on composite primary keys.
    - **3NF:** 2NF + No transitive dependencies on non-key attributes.
    - **BCNF:** Strict 3NF where every determinant is a candidate key.
    
    Intentional denormalization duplicates data across tables to eliminate expensive multi-table joins in high-volume read workloads, trading storage and write complexity for query latency.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q01RelationalNormalization.java"
        ```

### 2. How does a B-Tree index work internally and why is its search complexity $O(\log N)$?

??? question "Reveal answer"
    A B-Tree stores sorted keys in balanced multi-level 8 KB pages (root, branch, leaf). Searching starts at the root page, performs binary search to find the appropriate child branch pointer, and traverses down to the leaf page containing Tuple Identifiers (TIDs). A 3-level tree with fan-out of 500 can index over 125 million rows in just 3 page reads.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q02BTreeIndexInternals.java"
        ```

### 3. What is the Leftmost Prefix Rule for composite indexes?

??? question "Reveal answer"
    A composite index `(A, B, C)` can only be used for direct range and equality lookups if the query's `WHERE` clause filters on the leading (leftmost) column `A`. Filtering on `B` or `C` alone cannot traverse the B-Tree root and branch hierarchy directly, forcing full index scans or sequential scans.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q03CompositeIndexLeftmostRule.java"
        ```

### 4. What is a Covering Index (Index-Only Scan) and how does index selectivity affect plan selection?

??? question "Reveal answer"
    A covering index contains all columns requested by a query (using `CREATE INDEX ... INCLUDE (...)`). The database retrieves data entirely from B-Tree leaf pages without touching the heap table pages. When selectivity is high (matching $< 5\%$ of rows), the planner chooses Index Scans; when selectivity is low, it chooses Sequential Scans to minimize random I/O.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q04CoveringIndexSelectivity.java"
        ```

### 5. When should you use Partial Indexes and Functional/Expression Indexes?

??? question "Reveal answer"
    - **Partial Index:** Indexes rows matching a predicate (`WHERE status = 'PENDING'`). Ideal when only a small fraction of rows are actively queried, drastically reducing index size and write overhead.
    - **Functional Index:** Indexes the evaluated result of a function (`LOWER(email)`), enabling fast indexed lookups on case-insensitive or transformed columns.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q05PartialAndFunctionalIndexes.java"
        ```

### 6. How do you interpret an `EXPLAIN (ANALYZE, BUFFERS)` execution plan?

??? question "Reveal answer"
    `EXPLAIN` estimates query costs, while `ANALYZE` actually executes the query to output real elapsed execution time and row counts. `BUFFERS` reports I/O activity: `shared hit` indicates 8 KB pages retrieved directly from RAM (`shared_buffers`), while `read` indicates physical disk reads.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q06ExplainAnalyzeBuffers.java"
        ```
<!-- --8<-- [end:basic] -->

<!-- --8<-- [start:intermediate] -->
## Intermediate

### 7. Compare the three physical SQL join algorithms (Nested Loop, Hash Join, Merge Join).

??? question "Reveal answer"
    - **Nested Loop:** Iterates over the outer table and seeks inner table rows via an index. Best for small outer datasets with indexed inner keys.
    - **Hash Join:** Hashes the smaller relation in memory, then streams the larger relation to probe matching keys. Best for large unindexed equality joins.
    - **Merge Join:** Scans two pre-sorted relations simultaneously in linear time. Best when both datasets are already sorted by B-Tree indexes.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q07SqlJoinAlgorithms.java"
        ```

### 8. How do relational storage engines implement ACID guarantees?

??? question "Reveal answer"
    - **Atomicity & Durability:** Implemented via Write-Ahead Logging (WAL) and commit log records (`fsync` to disk).
    - **Consistency:** Enforced by constraint checks, schema rules, and foreign key validations.
    - **Isolation:** Implemented via Multi-Version Concurrency Control (MVCC) snapshot isolation and relational row/table locks.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q08AcidGuarantees.java"
        ```

### 9. What are the four database isolation levels and their corresponding anomalies?

??? question "Reveal answer"
    - **READ UNCOMMITTED:** Subject to Dirty Reads, Non-Repeatable Reads, and Phantoms.
    - **READ COMMITTED (Default in PG):** Prevents Dirty Reads; subject to Non-Repeatable Reads and Phantoms.
    - **REPEATABLE READ:** Prevents Dirty, Non-Repeatable, and Phantom Reads in PostgreSQL via transaction start snapshots; subject to Write Skew.
    - **SERIALIZABLE:** Prevents all anomalies including Write Skew via Serializable Snapshot Isolation (SSI).

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q09IsolationAnomalies.java"
        ```

### 10. How does PostgreSQL MVCC work and why is Autovacuum essential?

??? question "Reveal answer"
    PostgreSQL attaches `xmin` (creating TXID) and `xmax` (deleting/updating TXID) to every row header. Updating a row writes a new tuple version without in-place mutation. Autovacuum reclaims space occupied by dead tuples, updates the Free Space Map and Visibility Map, and prevents the 2-billion Transaction ID wraparound catastrophe.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q10MvccAndVacuumInternals.java"
        ```

### 11. How does the relational locking hierarchy operate in PostgreSQL?

??? question "Reveal answer"
    Locks exist at row, table, and advisory levels. `ACCESS SHARE` (acquired by `SELECT`) conflicts only with `ACCESS EXCLUSIVE` (acquired by `ALTER TABLE` / `DROP TABLE`). `ROW EXCLUSIVE` (acquired by `UPDATE`/`DELETE`) conflicts with table-level share locks. `SELECT ... FOR UPDATE` acquires row-level exclusive locks.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q11LockingHierarchy.java"
        ```

### 12. How does PostgreSQL detect deadlocks and how do you prevent them?

??? question "Reveal answer"
    When a lock wait exceeds `deadlock_timeout` (default: 1 second), PostgreSQL analyzes the active Wait-For Graph. If a directed cycle is found, it aborts one transaction with SQLSTATE `40P01`. Prevent deadlocks by enforcing **canonical resource ordering** (e.g. always acquiring locks in ascending primary key order).

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q12DeadlockDetectionAndResolution.java"
        ```

### 13. What is the difference between `SELECT FOR UPDATE`, `NOWAIT`, and `SKIP LOCKED`?

??? question "Reveal answer"
    - `FOR UPDATE`: Blocks until the locked row is released.
    - `FOR UPDATE NOWAIT`: Fails immediately with error `55P03` if the row is already locked.
    - `FOR UPDATE SKIP LOCKED`: Bypasses currently locked rows and returns the next available unlocked rows immediately, enabling lock-free distributed worker queues.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q13SelectForUpdateSkipLocked.java"
        ```

### 14. What are PostgreSQL Advisory Locks and when are they used?

??? question "Reveal answer"
    Advisory locks are application-defined locks identified by 64-bit integer keys. They exist independently of table data. Session-level advisory locks remain active until explicitly unlocked or connection closure; transaction-level advisory locks (`pg_advisory_xact_lock`) release automatically on commit/rollback. Used for distributed schedulers, singleton jobs, and Flyway migration coordination.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q14AdvisoryLocks.java"
        ```
<!-- --8<-- [end:intermediate] -->

<!-- --8<-- [start:senior] -->
## Senior

### 15. Why does `OFFSET` pagination fail at scale and how does Keyset pagination resolve it?

??? question "Reveal answer"
    `OFFSET N` requires the database to read and discard $N$ preceding rows off disk/cache, resulting in $O(N)$ linear latency degradation and data drift under concurrent inserts. Keyset pagination filters using indexed column criteria (`WHERE (created_at, id) < (:cursorCreatedAt, :cursorId)`), seeking directly to the next B-Tree leaf node in $O(\log N)$ time with constant $O(1)$ response time.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q15OffsetVsKeysetPagination.java"
        ```

### 16. How do Materialized Views work and how do you refresh them without locking?

??? question "Reveal answer"
    Standard views recompute SQL queries on every call, while Materialized Views store the query results physically on disk and support B-Tree indexing. Executing `REFRESH MATERIALIZED VIEW CONCURRENTLY` recalculates the view in the background while allowing concurrent `SELECT` queries, requiring a `UNIQUE` index on the materialized view.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q16MaterializedViews.java"
        ```

### 17. How does Table Partitioning improve query performance and maintenance?

??? question "Reveal answer"
    Table partitioning (by Range, List, or Hash) splits massive tables into independent physical child tables. **Partition Pruning** allows the query planner to bypass irrelevant partition tables entirely during query execution. Purging historical data is an instantaneous $O(1)$ metadata operation (`DROP TABLE partition_2020_01`), avoiding heavy WAL generation and dead tuple bloat.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q17TablePartitioning.java"
        ```

### 18. What is the Expand and Contract pattern for zero-downtime database migrations?

??? question "Reveal answer"
    A 4-phase migration pattern:
    1. **Expand:** Add the new column/table nullable without dropping the old one.
    2. **Backfill & Dual Write:** Deploy application version that writes to both columns and backfill historical rows.
    3. **Switch:** Deploy application reading from the new column.
    4. **Contract:** Drop old columns and database triggers once all legacy instances are retired.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q18ZeroDowntimeMigrations.java"
        ```

### 19. How do you calculate optimal database connection pool sizing (HikariCP formula)?

??? question "Reveal answer"
    Using the formula: $\text{connections} = ((\text{CPU Cores} \times 2) + \text{effective\_spindle\_count})$. For an 8-core server with SSD storage, an optimal pool size is $\approx 17$ connections. Oversizing connection pools causes CPU context switching thrashing, disk contention, and cache eviction, reducing overall system throughput.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q19ConnectionPoolSizing.java"
        ```

### 20. When should you use Common Table Expressions (CTEs) and Recursive Queries?

??? question "Reveal answer"
    Non-recursive CTEs (`WITH`) structure complex multi-step queries modularly and are inlined by PostgreSQL 12+ by default. Recursive CTEs (`WITH RECURSIVE`) enable graph traversal, hierarchical organizational trees, bill-of-materials resolution, and depth-first searches directly in SQL.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q20CommonTableExpressions.java"
        ```
<!-- --8<-- [end:senior] -->

<!-- --8<-- [start:scenarios] -->
## Production Scenarios

### 21. How do you design a high-throughput distributed worker queue in PostgreSQL using `SKIP LOCKED`?

??? question "Reveal answer"
    Use a single atomic query with a CTE:
    ```sql
    WITH next_job AS (
        SELECT id FROM tasks 
        WHERE status = 'PENDING' AND scheduled_at <= NOW()
        ORDER BY priority DESC, id ASC LIMIT 1
        FOR UPDATE SKIP LOCKED
    )
    UPDATE tasks SET status = 'PROCESSING', started_at = NOW()
    FROM next_job WHERE tasks.id = next_job.id
    RETURNING tasks.*;
    ```
    This enables hundreds of concurrent worker threads to pull distinct jobs simultaneously with zero lock contention.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q21DistributedJobQueueSkipLocked.java"
        ```

### 22. How do you diagnose and remediate table and index bloat in production?

??? question "Reveal answer"
    Diagnose bloat by monitoring `n_dead_tup` vs `n_live_tup` in `pg_stat_user_tables` and using the `pgstattuple` extension. Rebuild bloated B-Tree indexes online using `REINDEX CONCURRENTLY`, and reclaim fragmented table disk space using `pg_repack` without taking exclusive table locks.

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q22DatabaseBloatRemediation.java"
        ```

### 23. How do you migrate a 100-million row table to add a `NOT NULL` constraint without downtime?

??? question "Reveal answer"
    1. Add the column as nullable: `ALTER TABLE users ADD COLUMN age INT;` (Instant).
    2. Backfill existing rows in batches of 5,000 using keyset pagination to avoid long transactions.
    3. Add a check constraint without validating: `ALTER TABLE users ADD CONSTRAINT chk_age_not_null CHECK (age IS NOT NULL) NOT VALID;` (Instant).
    4. Validate constraint online: `ALTER TABLE users VALIDATE CONSTRAINT chk_age_not_null;` (Acquires `SHARE UPDATE EXCLUSIVE` lock, allowing concurrent reads and writes).

    ??? example "Example"
        ```java
        --8<-- "modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q23OnlineLargeTableMigration.java"
        ```
<!-- --8<-- [end:scenarios] -->
